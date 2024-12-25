package org.example.poll;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import org.example.Constants;
import org.example.database.QueryUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Poller extends AbstractVerticle
{
    private static final Logger logger = LoggerFactory.getLogger(Poller.class);

    public void start()
    {
        vertx.eventBus().<JsonObject>consumer(Constants.OBJECT_POLL, message ->
        {
            var pollingData = message.body();

            //Fetching credentials of qualified devices
            var columns = List.of("profile_protocol","user_name","user_password","community","version");

            //Fetching device details through credential profile ID as that details will also be needed
            QueryUtility.getInstance().get(Constants.CREDENTIALS,columns,new JsonObject().put("profile_id",pollingData.getLong("credential.profile")))
                    .onSuccess(deviceInfo->
                    {
                        pollingData.put("profile.protocol",deviceInfo.getString("profile_protocol"))
                                .put("user.name",deviceInfo.getString("user_name"))
                                .put("user.password",deviceInfo.getString("user_password"))
                                .put("community",deviceInfo.getString("community"))
                                .put("version",deviceInfo.getString("version"));

                        var timestamp = pollingData.getString("timestamp");

                        //As I don't require these 2 values in plugin , so I am separately passing the timestamp

                        pollingData.remove("credential.profile");

                        pollingData.remove("timestamp");

                        startPoll(pollingData,timestamp);
                    })
                    .onFailure(error ->
                    {
                        logger.error("Failed to fetch device details for profile_id: {}. Cause: {}",
                                pollingData.getLong("credential.profile"), error.getMessage());
                    });
        });
    }

    private void startPoll(JsonObject pollingData, String timestamp)
    {
        logger.info("Started polling of ip: {}",pollingData.getString("ip"));

        vertx.executeBlocking(promise ->
        {
            try
            {
                // Start the process
                pollingData.put(Constants.EVENT_TYPE,Constants.POLL);

                Process process = new ProcessBuilder(Constants.PLUGIN_PATH, pollingData.encode())
                        .redirectErrorStream(true).start();

                // Capture output from the Go executable
                var output = new String(process.getInputStream().readAllBytes());

                var exitCode = process.waitFor();

                if (exitCode != 0)
                {
                    logger.error("Go executable failed with error: {}", output.trim());

                    promise.fail(new RuntimeException("Polling failed"));
                }
                else
                {
                    logger.info("Metrics collected: {}", output.trim());

                    promise.complete(output.trim());
                }
            }
            catch (Exception exception)
            {
                logger.error("Failed to execute Go executable", exception);

                promise.fail(exception);
            }
        }, false,res ->
        {
            if (res.succeeded())
            {
                vertx.eventBus().request(Constants.FILE_WRITE,new JsonObject().put("ip",pollingData.getString("ip")).put("metric.group",pollingData.getString("metric.group.name")).put("metrics", res.result()).put("timestamp",timestamp),reply->{
                    if (reply.succeeded())
                    {
                        logger.info("Metrics stored successfully: {}", reply.result().body());
                    }
                    else
                    {
                        logger.error("Failed to store metrics for ip: {}", pollingData.getString("ip"), reply.cause());
                    }
                });

                logger.info("Metrics fetched successfully for ip: {}", pollingData.getString("ip"));
            }
            else
            {
                logger.error("Failed to fetch metrics for ip: {}", pollingData.getString("ip"));
            }
        });
    }
}
