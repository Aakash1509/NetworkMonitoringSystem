package org.example;

public class Constants
{
    public static final String BASE_DIRECTORY = System.getProperty("user.dir")+ "/storage";

    public static final int POOL_SIZE = 5;

    public static final int DB_PORT = 5432;

    public static final String DB_HOST = "localhost";

    public static final String DB_DATABASE = "project";

    public static final String DB_USER = "postgres";

    public static final String DB_PASSWORD = "test";

    public static final int HTTP_PORT = 8080;

    public static final String CREDENTIALS = "credentials";

    public static final String DISCOVERIES = "discoveries";

    public static final String METRICS = "metrics";

    public static final String OBJECTS = "objects";

    public static final String OBJECT_PROVISION = "object.provision";

    public static final String OBJECT_POLL = "object.poll";

    public static final String FILE_WRITE = "file.write";

    public static final int SNMP_POLL_INTERVAL = 20;

    public static final int INTERFACE_POLL_INTERVAL = 20;

    public static final int DEVICE_POLL_INTERVAL = 30;

    public static final int CPU_POLL_INTERVAL = 30;

    public static final int DISK_POLL_INTERVAL = 30;

    public static final int PROCESS_POLL_INTERVAL = 30;

    public static final int SSH_PORT = 22;


}
