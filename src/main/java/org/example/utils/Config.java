package org.example.utils;
import io.vertx.ext.web.RoutingContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Config
{
    public final static int POOL_SIZE = 5;

    public final static int DB_PORT = 5432;

    public final static String DB_HOST = "localhost";

    public final static String DB_DATABASE = "project";

    public final static String DB_USER = "postgres";

    public final static String DB_PASSWORD = "test";

    public final static int HTTP_PORT = 8080;

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

    public static Optional<String> validateFields(String discoveryName, String ip, Integer port)
    {
        //Integer,Long are wrapper classes so they can hold null value

        if (discoveryName == null || ip == null || port == null)
        {
            return Optional.of("Missing required fields in the request body.");
        }
        if(!validIp(ip))
        {
            return Optional.of("Invalid IP address");
        }
        if(validPort(port))
        {
            return Optional.of("Port number must be between 1 and 65535");
        }
        return Optional.empty(); //No error
    }

    public static void respond(RoutingContext context, int statusCode, String message)
    {
        context.response().setStatusCode(statusCode).end(message);
    }

    public static boolean ping(String ip)
    {
        try
        {
            var processBuilder = new ProcessBuilder("ping","-c 5",ip);

            var process = processBuilder.start();

            var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;

            boolean down = false;

            //Due to network latency if 5 packets are not send, then waitFor
            boolean status = process.waitFor(5, TimeUnit.SECONDS); //Will return boolean , while exitvalue returns 0 or other value

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
        Socket socket = new Socket();

        SocketAddress address = new InetSocketAddress(ip,port);

        try
        {
            socket.connect(address,2000);

//            System.out.println("Port is open");

            return true; //Port is open
        }
        catch (Exception exception)
        {
//            System.out.println("Port is closed");

            return false; //Port is closed
        }
        finally
        {
            try
            {
                socket.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

}
