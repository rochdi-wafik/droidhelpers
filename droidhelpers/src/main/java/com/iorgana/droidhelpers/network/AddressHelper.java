package com.iorgana.droidhelpers.network;

import android.util.Patterns;

import androidx.annotation.Nullable;

import com.iorgana.droidhelpers.utils.JPatterns;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;

/**
 * ************************************************************************
 * AddressHelper
 * ************************************************************************
 * Helper methods for IP address and network address operations.
 */
public class AddressHelper {

    /**
     * ************************************************************************
     * getIPAddress()
     * ************************************************************************
     * - Get the local (private) IP address of the device.
     * ------------------------------------------------------------------------
     * @param useIPv6 If true, returns IPv6 address; otherwise returns IPv4.
     * @return The IP address string, or empty string if not found.
     */
    public static String getIPAddress(Boolean useIPv6) {
        try {
            boolean isIPv4;
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface netInterface : interfaces) {
                List<InetAddress> addrList = Collections.list(netInterface.getInetAddresses());
                for (InetAddress addr : addrList) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        assert sAddr != null;
                        isIPv4 = sAddr.indexOf(':') < 0;

                        if(useIPv6){
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                        else {
                            if (isIPv4)
                                return sAddr;
                        }

                    }
                }
            }
        } catch (Exception ignored) {
        } // for now eat exceptions
        return "";
    }



    /**
     * ************************************************************************
     * generateRandomIP()
     * ************************************************************************
     * - Generate a random local (private) IP address.
     * - Keeps the network address and generates a random host address.
     * - Ensures the generated IP is not in the excluded list.
     * ------------------------------------------------------------------------
     * @param networkHost The network prefix (e.g., "192.168"), or null for default.
     * @param excludedIPs List of IPs to exclude.
     * @return A unique random IP address string.
     */
    public static String generateRandomIP(String networkHost, List<String> excludedIPs){
        // Generate New IP while its exists in the List, until found a unique IP
        Random random = new Random();
        int octet3, octet4;
        String ipAddress;
        String networkIp = (networkHost==null)? "192.168" : networkHost;

        do {
            octet3 = random.nextInt(255) + 1;
            octet4 = random.nextInt(255) + 1;

            ipAddress = networkIp+"." + octet3 + "." + octet4;
        } while (excludedIPs.contains(ipAddress));

        return ipAddress;
    }

    /**
     * ************************************************************************
     * getTakenIPs()
     * ************************************************************************
     * - Get a list of all IP addresses assigned to the device's network
     *   interfaces.
     * ------------------------------------------------------------------------
     * @return List of IP address strings.
     */
    public static List<String> getTakenIPs(){
        List<String> takenIp = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();

                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    String ipAddress = inetAddress.getHostAddress();

                    // Print the IP address
                    takenIp.add(ipAddress);
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return takenIp;
    }

    /**
     * ************************************************************************
     * isIp()
     * ************************************************************************
     * - Check if a string is a valid IP address.
     * ------------------------------------------------------------------------
     * @param ip The string to check.
     * @return true if valid IP, false otherwise.
     */
    public static boolean isIp(String ip){
        return JPatterns.IP_ADDRESS.matcher(ip).matches();
    }


    /**
     * ************************************************************************
     * isDomain()
     * ************************************************************************
     * - Check if a string is a valid domain name.
     * ------------------------------------------------------------------------
     * @param domain The string to check.
     * @return true if valid domain, false otherwise.
     */
    public static boolean isDomain(String domain){
        return JPatterns.DOMAIN_NAME.matcher(domain).matches();
    }

    /**
     * ************************************************************************
     * isWebUrl()
     * ************************************************************************
     * - Check if a string is a valid web URL.
     * ------------------------------------------------------------------------
     * @param url The string to check.
     * @return true if valid web URL, false otherwise.
     */
    public static boolean isWebUrl(String url){
        return JPatterns.WEB_URL.matcher(url).matches();
    }

    /**
     * ************************************************************************
     * isValidCidr()
     * ************************************************************************
     * - Check if a string is a valid CIDR notation (e.g., "0.0.0.0/0").
     * ------------------------------------------------------------------------
     * @param cidr The CIDR string to check.
     * @return true if valid CIDR, false otherwise.
     */
    public static boolean isValidCidr(String cidr) {
        String pattern = "^([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})/(\\d{1,2})$";
        return cidr.matches(pattern);
    }

    /**
     * ************************************************************************
     * isIpReachable()
     * ************************************************************************
     * - Check if an IP address is reachable via socket connection on port 53.
     * ------------------------------------------------------------------------
     * @param dnsIP     The IP address to check.
     * @param timeout_ms The total timeout in milliseconds (default 5000).
     * @return true if reachable, false otherwise.
     */
    public static boolean isIpReachable(String dnsIP,Integer timeout_ms){
        int port = 53;
        // timeouts
        final int sockTimeout = 800;
        final int totalTimeout = (timeout_ms!=null) ? timeout_ms : 5000;

        AtomicBoolean isReachable = new AtomicBoolean(); // default=false
        ExecutorService executor;

        executor = Executors.newSingleThreadExecutor();

        // start scan
        executor.submit(()->{
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(dnsIP, port), sockTimeout);
                socket.close();

                // if one port opened: break
                isReachable.set(true);
                executor.shutdownNow();
            }
            catch (IOException ignored) {}
        });

        executor.shutdown();

        // set total timeout
        try {
            executor.awaitTermination(totalTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {}

        return isReachable.get();
    }

    /**
     * ************************************************************************
     * getDomain()
     * ************************************************************************
     * - Extract the domain name from a URL.
     * ------------------------------------------------------------------------
     * @param url       The URL to extract from.
     * @param with_port If true, includes the port number in the result.
     * @return The domain name string, or null if not found.
     */
    public static String getDomain(String url, @Nullable Boolean with_port){

        Matcher matcher;
        if(with_port!=null && with_port){
            matcher = JPatterns._DOMAIN_WITH_PORT.matcher(url);
        }
        else{
            matcher = Patterns.DOMAIN_NAME.matcher(url);
        }
        return matcher.find() ?  matcher.group() : null;

    }
}