package com.iorgana.droidhelpers_project.ui.demo;

import android.widget.EditText;
import android.widget.LinearLayout;

import com.iorgana.droidhelpers.converters.DataSizeConverter;
import com.iorgana.droidhelpers.converters.JsonConverter;
import com.iorgana.droidhelpers_project.ui.base.BaseDemoActivity;

import java.util.Locale;

/**
 * ConvertersActivity
 * -----------------------------------------------------------------------------
 * Live usage examples for com.iorgana.droidhelpers.converters package:
 * JsonConverter (snake_case <-> camelCase) and DataSizeConverter (byte units,
 * human-readable formatting, running max-speed tracking).
 */
public class ConvertersActivity extends BaseDemoActivity {

    @Override
    protected String getScreenTitle() {
        return "Converters";
    }

    @Override
    protected void buildContent() {
        buildJsonConverterSection();
        buildDataSizeConverterSection();
    }

    /* ------------------------------------------------------------------ */
    private void buildJsonConverterSection() {
        LinearLayout s = addSection("JsonConverter", "Recursively converts JSON key casing, useful for snake_case APIs <-> Java models.");

        EditText snakeJsonInput = addInput(s, "snake_case JSON", "{\"user_name\":\"Sami\",\"is_admin\":true}");
        EditText camelJsonInput = addInput(s, "camelCase JSON", "{\"userName\":\"Sami\",\"isAdmin\":true}");
        EditText varInput = addInput(s, "Variable name", "userFullName");

        runSafe(addRow(s, "convertKeysToJavaCase(jsonString)  (snake_case -> camelCase)"), () ->
                JsonConverter.convertKeysToJavaCase(snakeJsonInput.getText().toString()));

        runSafe(addRow(s, "convertKeysToJsonCase(jsonString)  (camelCase -> snake_case)"), () ->
                JsonConverter.convertKeysToJsonCase(camelJsonInput.getText().toString()));

        runSafe(addRow(s, "javaToJsonCase(variableName)"), () -> JsonConverter.javaToJsonCase(varInput.getText().toString()));
        runSafe(addRow(s, "jsonToJavaCase(variableName)"), () -> JsonConverter.jsonToJavaCase("user_full_name"));
    }

    /* ------------------------------------------------------------------ */
    private void buildDataSizeConverterSection() {
        LinearLayout s = addSection("DataSizeConverter", "Byte-unit conversions, human-readable formatting, and running max-speed tracking.");

        EditText bytesInput = addInput(s, "Bytes value", "15728640");
        DataSizeConverter converter = new DataSizeConverter();

        runSafe(addRow(s, "getTopUploadSpeed(Double) / getTopDownloadSpeed(Double)  (tracks running max)"), () -> {
            double bytes = Double.parseDouble(bytesInput.getText().toString());
            double up = converter.getTopUploadSpeed(bytes);
            double down = converter.getTopDownloadSpeed(bytes * 0.5);
            return "topUpload=" + up + ", topDownload=" + down;
        });

        runSafe(addRow(s, "getTopUploadSpeed(Long) / getTopDownloadSpeed(Long)  (long overloads)"), () -> {
            long bytes = (long) Double.parseDouble(bytesInput.getText().toString());
            long up = converter.getTopUploadSpeed(bytes);
            long down = converter.getTopDownloadSpeed(bytes / 2);
            return "topUpload=" + up + ", topDownload=" + down;
        });

        runSafe(addRow(s, "getTopTotalSpeed(upload, download)"), () -> {
            long bytes = (long) Double.parseDouble(bytesInput.getText().toString());
            return String.valueOf(converter.getTopTotalSpeed(bytes, bytes / 2));
        });

        runSafe(addRow(s, "resetMaxSpeeds()"), () -> {
            converter.resetMaxSpeeds();
            return "All running max values reset to 0";
        });

        runSafe(addRow(s, "byteToString(Double) / byteToString(Long)  (human readable)"), () -> {
            double bytes = Double.parseDouble(bytesInput.getText().toString());
            return DataSizeConverter.byteToString((Double) bytes) + "  |  " + DataSizeConverter.byteToString((Long) (long) bytes);
        });

        runSafe(addRow(s, "shortDouble(value, numToKeep) / shortDouble(value, numToKeep, locale)"), () ->
                DataSizeConverter.shortDouble(3.14159, 2) + "  |  "
                        + DataSizeConverter.shortDouble(3.14159, 3, Locale.ENGLISH));

        runSafe(addRow(s, "removeCharsAfterDot(input, charsToKeep)"), () ->
                DataSizeConverter.removeCharsAfterDot("6.33378GB", 1));

        runSafe(addRow(s, "bytesToGigabytes(Double) / bytesToGigabytes(Long)"), () -> {
            double bytes = Double.parseDouble(bytesInput.getText().toString());
            return DataSizeConverter.bytesToGigabytes((Double) bytes) + "  |  " + DataSizeConverter.bytesToGigabytes((Long) (long) bytes);
        });

        runSafe(addRow(s, "bytesToMegabytes(Double) / bytesToMegabytes(Long)"), () -> {
            double bytes = Double.parseDouble(bytesInput.getText().toString());
            return DataSizeConverter.bytesToMegabytes((Double) bytes) + "  |  " + DataSizeConverter.bytesToMegabytes((Long) (long) bytes);
        });

        runSafe(addRow(s, "bytesToKilobytes(Double)"), () ->
                String.valueOf(DataSizeConverter.bytesToKilobytes(Double.parseDouble(bytesInput.getText().toString()))));

        runSafe(addRow(s, "gigabytesToBytes(Double) / megabytesToBytes(Double) / kilobytesToBytes(Double)"), () ->
                "GB->B=" + DataSizeConverter.gigabytesToBytes(1.0)
                        + ", MB->B=" + DataSizeConverter.megabytesToBytes(1.0)
                        + ", KB->B=" + DataSizeConverter.kilobytesToBytes(1.0));
    }
}
