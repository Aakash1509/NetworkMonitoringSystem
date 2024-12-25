package org.example.util;

import io.vertx.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.example.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class Helper
{
    private static final Logger logger = LoggerFactory.getLogger(Helper.class);

    public static boolean validIp(String ip)
    {
        var regex = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$";

        var pattern = Pattern.compile(regex);

        var matcher = pattern.matcher(ip);

        return matcher.matches();

    }

    public static boolean validPort(int port)
    {
        return port <= 1 || port >= 65535;
    }

    public static boolean ping(String ip)
    {
        try
        {
            var processBuilder = new ProcessBuilder("ping","-c 5",ip);

            var process = processBuilder.start();

            var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String  line;

            var down = false;

            //Due to network latency if 5 packets are not send, then waitFor
            var status = process.waitFor(5, TimeUnit.SECONDS); //Will return boolean , while exitvalue returns 0 or other value

            if(!status)
            {
                process.destroy();

                return false;
            }

            while((line = reader.readLine())!=null)
            {
                if(line.contains("100% packet loss"))
                {
                    down = true;

                    break;
                }
            }
            //If status is true , but exit value can be 1
            if(process.exitValue()!=0)
            {
                return false;
            }
            return !down; //Will return true , if no issues were detected
        }
        catch (Exception exception)
        {
            return false;
        }
    }

    public static boolean isPortOpen(String ip,Integer port)
    {
        var socket = new Socket();

        var address = new InetSocketAddress(ip,port);

        try
        {
            socket.connect(address,2000);

            return true; //Port is open
        }
        catch (Exception exception)
        {
            return false; //Port is closed
        }
        finally
        {
            try
            {
                socket.close();
            }
            catch (Exception exception)
            {
                logger.error(exception.getMessage());
            }
        }
    }

    public static void checkConnection(JsonObject deviceInfo)
    {
        try
        {
            // Setting event type to discover
            deviceInfo.put(Constants.EVENT_TYPE,Constants.DISCOVER);

            // Spawning a process
            Process process = new ProcessBuilder(Constants.PLUGIN_PATH, deviceInfo.encode())
                    .redirectErrorStream(true).start();

            // Wait for the process to complete within 60 seconds
            var status = process.waitFor(60, TimeUnit.SECONDS); //(boolean)

            if (!status)
            {
                process.destroy();

                logger.warn("Connection check timed out");

                deviceInfo.put("credential_profile",null);

                deviceInfo.put("hostname",null);

                deviceInfo.put("status", "Down");

                return;
                // Terminate the process if it times out
            }

            // Output from the Go executable
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String output = reader.readLine();

            logger.info("Output from Go executable: {}", output);

            // Parse the output and update the deviceInfo JSON object

            JsonObject result = new JsonObject(output);

            deviceInfo.put("credential_profile", result.getLong("credential.profile.id"));

            deviceInfo.put("hostname", result.getString("hostname") != null ? result.getString("hostname").trim() : null);

            deviceInfo.put("status", result.getString("status") != null ? result.getString("status").trim() : null);
        }
        catch (Exception exception)
        {
            logger.error("Error during connection making: {}", exception.getMessage());
        }
    }
}
