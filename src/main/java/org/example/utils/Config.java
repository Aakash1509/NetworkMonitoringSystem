package org.example.utils;

import io.vertx.ext.web.RoutingContext;

import java.util.Optional;
import java.util.regex.Pattern;

public class Config
{
    public static int POOL_SIZE=5;

    public static int DB_PORT = 3306;

    public static String DB_HOST = "localhost";

    public static String DB_DATABASE = "project";

    public static String DB_USER = "root";

    public static String DB_PASSWORD = "root";

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
}
