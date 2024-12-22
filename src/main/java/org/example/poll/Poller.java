package org.example.poll;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import org.example.Bootstrap;
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
        Bootstrap.vertx.eventBus().<JsonObject>consumer(Constants.OBJECT_POLL, message ->
        {
            var pollingData = message.body();

            var columns = List.of("profile_protocol","user_name","user_password","community","version");

            //Fetching device details through credential profile ID as that details will also be needed
            QueryUtility.getInstance().get("credentials",columns,new JsonObject().put("profile_id",pollingData.getLong("credential.profile")))
                    .onSuccess(deviceInfo->
                    {
                        pollingData.put("profile.protocol",deviceInfo.getString("profile_protocol"))
                                .put("user.name",deviceInfo.getString("user_name"))
                                .put("user.password",deviceInfo.getString("user_password"))
                                .put("community",deviceInfo.getString("community"))
                                .put("version",deviceInfo.getString("version"));

                        pollingData.remove("credential.profile");

                        startPoll(pollingData);
                    })
                    .onFailure(error ->
                    {
                        logger.error("Failed to fetch device details for profile_id: {}. Cause: {}",
                                pollingData.getLong("credential.profile"), error.getMessage());
                    });
        });
    }

    private void startPoll(JsonObject pollingData)
    {
        logger.info("Started polling of ip: {}",pollingData.getString("ip"));

        Bootstrap.vertx.executeBlocking(promise ->
        {
            try
            {
                // Start the process
                Process process = new ProcessBuilder("/home/aakash/Plugin/polling/polling", pollingData.encode())
                        .redirectErrorStream(true).start();

                // Capture output from the Go executable
                var output = new String(process.getInputStream().readAllBytes());

                var error = new String(process.getErrorStream().readAllBytes());

                process.waitFor();

                if (!error.isEmpty())
                {
                    logger.error("Error from Go executable: {}", error);
                }
                else
                {
                    logger.info("Metrics collected: {}", output);
                }
                promise.complete(output);
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
                logger.info("Metrics fetched successfully for ip: {}", pollingData.getString("ip"));
            }
            else
            {
                logger.error("Failed to fetch metrics for ip: {}", pollingData.getString("ip"));
            }
        });
    }
}
