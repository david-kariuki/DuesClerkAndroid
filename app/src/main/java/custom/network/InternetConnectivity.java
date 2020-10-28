package custom.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;

import custom.custom_utilities.DataUtils;

/**
 * This class checks for device's network connectivity and the network speed
 * WiFi network speed is calculated by downloading a file and calculating the network speed
 *
 * @author David Kariuki
 */
@SuppressWarnings({"unused", "RedundantSuppression"})
public class InternetConnectivity {

    // Get activity simple name
    private static final String TAG = InternetConnectivity.class.getSimpleName();
    private static final int byteValue = 1000;
    private static HashMap<String, String> connectionSpeedInfo;

    /**
     * Check for internet connection
     *
     * @param context - For getting network info
     * @return boolean
     */
    public static boolean isConnectedToAnyNetwork(Context context) {
        NetworkInfo info = InternetConnectivity.getNetworkInfo(context);
        return (info != null && info.isConnected());
    }

    /**
     * Get network info
     *
     * @param context - For getting connectivity service
     * @return NetworkInfo
     */
    private static NetworkInfo getNetworkInfo(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return Objects.requireNonNull(connectivityManager).getActiveNetworkInfo();
    }

    /**
     * Check for WiFi network connection
     *
     * @param context - For getting network info
     * @return return
     */
    public static boolean isConnectedToWifiNetwork(Context context) {
        NetworkInfo info = InternetConnectivity.getNetworkInfo(context);
        return (info != null && info.isConnected() && info.getType() ==
                ConnectivityManager.TYPE_WIFI);
    }

    /**
     * Check for Mobile network connection
     *
     * @param context - For getting network info
     * @return boolean
     */
    public static boolean isConnectedToMobileNetwork(Context context) {
        NetworkInfo info = InternetConnectivity.getNetworkInfo(context);
        return (info != null && info.isConnected() && info.getType() ==
                ConnectivityManager.TYPE_MOBILE);
    }

    /**
     * Check for VPN network connection
     *
     * @param context - For getting network info
     * @return boolean
     */
    public static boolean isConnectedToVPNNetwork(Context context) {
        NetworkInfo info = InternetConnectivity.getNetworkInfo(context);
        return (info != null && info.isConnected() && info.getType() ==
                ConnectivityManager.TYPE_VPN);
    }

    /**
     * Check for fast connection
     *
     * @param context - For getting network info
     * @return boolean
     */
    public static boolean isConnectionFast(Context context) {
        NetworkInfo info = InternetConnectivity.getNetworkInfo(context);
        return (info != null && info.isConnected()
                && InternetConnectivity.isConnectionFast(info.getType(), info.getSubtype()));
    }

    /**
     * Check for fast connection with specific type and subType
     *
     * @param type    - networkType
     * @param subType - networkSubType
     * @return boolean
     */
    public static boolean isConnectionFast(int type, int subType) {
        if (type == ConnectivityManager.TYPE_WIFI || type == ConnectivityManager.TYPE_VPN) {
            // Calculate WiFi network speed - (WiFi is not always fast)
            return calculateNetworkSpeedByDownloadingFile();
        } else if (type == ConnectivityManager.TYPE_MOBILE) {
            switch (subType) {
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    return false; // ~ 50-100 Kbps - slow.
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    return false; // ~ 14-64 Kbps - slow.
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    return true;  // ~ 400-1000 Kbps
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    return true;  // ~ 600-1400 Kbps
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    return false; // ~ 100 Kbps - slow.
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                    return true;  // ~ 2000-1400 Kbps
                case TelephonyManager.NETWORK_TYPE_HSPA:
                    return true;  // ~ 700-1700 Kbps
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                    return true;  // ~ 1000-2300 Kbps
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    return true;  // ~ 400-7000 Kbps
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                    return true;  // ~ 1000-2000 Kbps - API level 11
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                    return true;  // ~ 5000 Kbps - API level 9
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    return true;  // ~ 10000-20000 Kbps - API level 13
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    return false; // ~ 25 Kbps - API level 8
                case TelephonyManager.NETWORK_TYPE_LTE:
                    return true;  // ~ 10000+ Kbps - API level 11
                case TelephonyManager.NETWORK_TYPE_UNKNOWN: // ~ Unknown
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Calculate network speed by downloading file
     *
     * @return networkState
     */
    private static boolean calculateNetworkSpeedByDownloadingFile() {
        long startTime;
        final long[] endTime = new long[1];
        long[] testFileSize = new long[1];
        OkHttpClient client = new OkHttpClient();
        final boolean[] isFastNetwork = {false};

        // Create and build request
        Request request = new Request.Builder()
                // Url of image or file to be downloaded
                .url("https://github.com/david-kariuki/AndroidInternetConnectivity/blob/master" +
                        "/test_download_image.png").build(); // Build request

        // Get start time in milli seconds
        startTime = System.currentTimeMillis();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (!response.isSuccessful()) { // Response not successful
                    switch (response.code()) { // Switch error codes
                        case 401:
                            throw new IOException("Unauthorized " + response.code());
                        case 404:
                            throw new IOException("Not found " + response.code());
                        case 408:
                            throw new IOException("Request Timeout " + response.code());
                        case 429:
                            throw new IOException("Too Many Requests " + response.code());
                        case 444:
                            throw new IOException("Connection Closed Without Response "
                                    + response.code());
                        case 500:
                            throw new IOException("Internal Server Error "
                                    + response.code());
                        default:
                            throw new IOException("Unexpected code "
                                    + response.code() + " " + response);
                    }
                }

                // Get response headers
                Headers responseHeaders = response.headers();

                try (InputStream input = response.body().byteStream()) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[byteValue];

                    while (input.read(buffer) != -1) {
                        byteArrayOutputStream.write(buffer);
                    }
                    testFileSize[0] = byteArrayOutputStream.size();
                }

                // Set end time
                endTime[0] = System.currentTimeMillis();

                double timeTakenMillis, timeTakenSecs, downloadSpeed, bytesPerSec, kilobytesPerSec,
                        megabytesPerSec;

                // Calculate download time by subtracting endTime from startTime
                timeTakenMillis = Math.floor(endTime[0] - startTime);  // time taken in milliseconds

                if (timeTakenMillis > 0) { // Network speed calculated

                    timeTakenSecs = timeTakenMillis / 1000; // Get time in seconds
                    // Get kilobytes per second
                    kilobytesPerSec = DataUtils.roundDouble((byteValue / timeTakenSecs), 2);
                    // Get bytes per second
                    bytesPerSec = DataUtils.roundDouble((kilobytesPerSec * byteValue), 2);
                    // Get megabytes per second
                    megabytesPerSec = DataUtils.roundDouble((kilobytesPerSec / byteValue), 2);

                    // Bandwidth in Kbps
                    // POOR -      Bandwidth under 100 Kbps.
                    // MODERATE    Bandwidth between 150 and 550 Kbps.
                    // GOOD        Bandwidth over 2000 Kbps.
                    // EXCELLENT   Bandwidth over 2000 Kbps.
                    // UNKNOWN     Connection quality cannot be found.

                    isFastNetwork[0] = (kilobytesPerSec > 100); // Update connection speed state

                    // Get the download speed by dividing the file size by time taken to download
                    downloadSpeed = DataUtils.roundDouble((testFileSize[0] / timeTakenMillis), 2);

                    connectionSpeedInfo = new HashMap<>();
                    connectionSpeedInfo.put("timeTakenMillis", String.valueOf(timeTakenMillis));
                    connectionSpeedInfo.put("timeTakenSecs", String.valueOf(timeTakenSecs));
                    connectionSpeedInfo.put("linkSpeedBps", String.valueOf(bytesPerSec));
                    connectionSpeedInfo.put("linkSpeedKbps", String.valueOf(kilobytesPerSec));
                    connectionSpeedInfo.put("linkSpeedMbps", String.valueOf(megabytesPerSec));
                    connectionSpeedInfo.put("testFileDownloadSpeed", String.valueOf(downloadSpeed));
                    connectionSpeedInfo.put("testFileSize", String.valueOf(testFileSize[0]));
                    connectionSpeedInfo.put("isFastNetwork", String.valueOf(isFastNetwork[0]));
                }
            }
        });
        // Return connection state
        return isFastNetwork[0];
    }

    /**
     * Check for fast WiFi connection
     * Returns detailed information like time taken in milliseconds, seconds, networks speed
     * (in bytes, kbps and mbps), download speed and test file size.
     *
     * @return downloadResponse information for debugging purposes or more specific checks
     */
    public static HashMap<String, String> getWiFiConnectionSpeedInfo(Context context) {
        if (getNetworkInfo(context).getType() == ConnectivityManager.TYPE_WIFI) {
            calculateNetworkSpeedByDownloadingFile();
        }
        return connectionSpeedInfo;
    }

    /**
     * Check for fast VPN connectio.n
     * Returns detailed information like time taken in milliseconds, seconds, networks speed
     * (in bytes, kbps and mbps), download speed and test file size.
     *
     * @return downloadResponse information for debugging purposes or more specific checks
     */
    public static HashMap<String, String> getVPNConnectionSpeedInfo(Context context) {
        if (getNetworkInfo(context).getType() == ConnectivityManager.TYPE_VPN) {
            calculateNetworkSpeedByDownloadingFile();
        }
        return connectionSpeedInfo;
    }
}
