package org.example.poll;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import org.example.Constants;
import org.example.database.QueryUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Poller extends AbstractVerticle //Logic will be here only to update the last_polled time of the device
{
    private static final Logger logger = LoggerFactory.getLogger(Poller.class);

    public void start()
    {
        vertx.eventBus().<JsonObject>consumer(Constants.OBJECT_POLL, message ->
        {
            var pollingData = message.body();

            List<String> columns = List.of("profile_protocol","user_name","user_password","community","version");

            //Fetching device details through credential profile ID as that details will also be needed
            QueryUtility.getInstance().get("credentials",columns,new JsonObject().put("profile_id",pollingData.getLong("credential.profile")))
                    .onSuccess(deviceInfo->
                    {
                        pollingData.put("profile.protocol",deviceInfo.getString("profile_protocol"))
                                .put("user.name",deviceInfo.getString("user_name"))
                                .put("user.password",deviceInfo.getString("user_password"))
                                .put("community",deviceInfo.getString("community"))
                                .put("version",deviceInfo.getString("version"));

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
        logger.info(pollingData.encodePrettily());
    }
}
