package org.example.services;

import io.vertx.core.AbstractVerticle;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.example.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileWriter extends AbstractVerticle
{
    private static final Logger logger = LoggerFactory.getLogger(FileWriter.class);

    @Override
    public void start()
    {
        vertx.eventBus().<JsonObject>consumer(Constants.FILE_WRITE, message ->
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

                var filePath = Constants.BASE_DIRECTORY + "/" + String.format("%s.txt", timestamp);

                var finalMetrics = metrics; //As metrics is been modified inside lambda expression so need to make it effectively final

                vertx.fileSystem().exists(filePath, existsResult ->
                {
                    if (existsResult.succeeded() && existsResult.result())
                    {
                        // File exists, append the data
                        appendToFile(filePath, metricName, finalMetrics, ip, message);
                    }
                    else
                    {
                        // File does not exist, create a new one
                        writeFile(filePath, metricName, finalMetrics, ip, message);
                    }
                });
            }
            catch (Exception exception)
            {
                logger.error("Exception occurred while handling file write: {}", exception.getMessage(), exception);

                message.fail(1, "Exception occurred: " + exception.getMessage());
            }
        });
    }

    private void appendToFile(String filePath, String metricName, Object metrics, String ip, io.vertx.core.eventbus.Message<JsonObject> message)
    {
        vertx.fileSystem().readFile(filePath, readResult ->
        {
            if (readResult.succeeded())
            {
                var existingData = readResult.result().toString();

                var separator = "\n" + "----------------------" + "\n"; // Separator

                var newData = new JsonObject()
                        .put("ip", ip)
                        .put("result", new JsonObject().put(metricName, metrics))
                        .encodePrettily();

                // Append the new data with a separator
                vertx.fileSystem().writeFile(filePath, Buffer.buffer(existingData + separator + newData), writeResult ->
                {
                    if (writeResult.succeeded())
                    {
                        message.reply("Data appended successfully to: " + filePath);
                    }
                    else
                    {
                        logger.error("Failed to append data to file: {}", writeResult.cause().getMessage());

                        message.fail(1, "Failed to append data: " + writeResult.cause().getMessage());
                    }
                });
            }
            else
            {
                logger.error("Failed to read the existing file for appending: {}", readResult.cause().getMessage());

                message.fail(1, "Failed to read the existing file: " + readResult.cause().getMessage());
            }
        });
    }

    private void writeFile(String filePath, String metricName, Object metrics, String ip, io.vertx.core.eventbus.Message<JsonObject> message)
    {
        vertx.fileSystem().writeFile(filePath,
                Buffer.buffer(new JsonObject()
                        .put("ip", ip)
                        .put("result", new JsonObject().put(metricName, metrics))
                        .encodePrettily()),
                writeResult ->
                {
                    if (writeResult.succeeded())
                    {
                        message.reply("File written successfully: " + filePath);
                    }
                    else
                    {
                        logger.error("Failed to write file: {}", writeResult.cause().getMessage());

                        message.fail(1, "Failed to write file: " + writeResult.cause().getMessage());
                    }
                });
    }
}
