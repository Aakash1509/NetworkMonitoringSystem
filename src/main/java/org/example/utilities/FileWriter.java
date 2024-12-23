package org.example.utilities;

import io.vertx.core.AbstractVerticle;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.example.Bootstrap;
import org.example.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileWriter extends AbstractVerticle
{
    private static final Logger logger = LoggerFactory.getLogger(FileWriter.class);

    @Override
    public void start()
    {
        Bootstrap.vertx.eventBus().<JsonObject>consumer(Constants.FILE_WRITE, message ->
        {
            try
            {
                var data = message.body();

                var ip = data.getString("ip");

                var metricName = data.getString("metric.group");

                var metrics = data.getValue("metrics");

                var timestamp = data.getString("timestamp");

                if (metrics instanceof String metricsString)
                {
                    try
                    {
                        if (metricsString.trim().startsWith("{"))
                        {
                            metrics = new JsonObject(metricsString);  // Parse as JsonObject
                        }
                        else
                        {
                            metrics = new JsonArray(metricsString);  // Parse as JsonArray
                        }
                    }
                    catch (Exception exception)
                    {
                        logger.error("Failed to parse metrics string: {}", exception.getMessage());

                        message.fail(1, "Invalid JSON format for metrics");

                        return;
                    }
                }
                vertx.fileSystem().writeFile(Constants.BASE_DIRECTORY + "/" + String.format("%s_%s.txt", ip, timestamp.replace("T", " ")),
                        Buffer.buffer(new JsonObject().put("result", new JsonObject().put(metricName, metrics)).encodePrettily()),
                        writeResult ->
                        {
                            if (writeResult.succeeded())
                            {
                                message.reply("File written successfully: " + ip + "_" + timestamp.replace("T", " ") + ".txt");
                            }
                            else
                            {
                                logger.error("Failed to write file: {}", writeResult.cause().getMessage());

                                message.fail(1, "Failed to write file: " + writeResult.cause().getMessage());
                            }
                        });
            }
            catch (Exception e)
            {
                logger.error("Exception occurred while handling file write: {}", e.getMessage(), e);

                message.fail(1, "Exception occurred: " + e.getMessage());
            }
        });
    }

}
