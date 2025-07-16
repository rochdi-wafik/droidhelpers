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

public class AddressHelper {

    /**
     * Get IP Address Of Device
     * ---------------------------------------------------------------------------------
     * This method return the local (private) ip address of this device
     * IF useIPv6 = true: return ip version 6
     * IF useIPv6 = false or null: return ip version 4
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
     * Generate Unique IP Address
     * ---------------------------------------------------------------------
     * - The output IP is a local IP (private)
     * - The generated IP is not used in the passed List of IPs
     * - The output IP contains two parts:
     * - Network Address: "192.168" which is not changed
     * - Host Address: which is changed
     * - This method will generate a random Host Address, and keeping the Network Address
     * - Example: "192.168.10.1"
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
     * Get A List Of Taken IP Addresses
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
     * Is IP
     * @param ip:
     * @return boolean
     */
    public static boolean isIp(String ip){
        return JPatterns.IP_ADDRESS.matcher(ip).matches();
    }


    /**
     * Is Domain
     * @param domain:
     * @return boolean
     */
    public static boolean isDomain(String domain){
        return JPatterns.DOMAIN_NAME.matcher(domain).matches();
    }

    // Is WebUrl
    public static boolean isWebUrl(String url){
        return JPatterns.WEB_URL.matcher(url).matches();
    }

    /**
     * Is CIDR
     * ------------------------------------------------------------------
     * cidr represent a network route, like this: 0.0.0.0/0
     */
    public static boolean isValidCidr(String cidr) {
        String pattern = "^([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})/(\\d{1,2})$";
        return cidr.matches(pattern);
    }

    /**
     * Is IP Reachable
     * -------------------------------------------------------------------
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
     * Extract domain name from address
     * @param url address
     * @param with_port with_port - if true: return domain with port if found
     * @return string domain name
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
