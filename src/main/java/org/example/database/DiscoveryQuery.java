package org.example.database;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import static org.example.Bootstrap.client;

public class DiscoveryQuery
{
    //Query for creating discovery
    public Future<Long> insert(String name, String ip, Integer port, JsonArray credential_profiles,String status)
    {
        Promise<Long> promise = Promise.promise();

        client.preparedQuery("INSERT INTO discoveries (name, ip, port, credential_profiles, status) " +
                        "VALUES ($1, $2, $3, $4, $5) RETURNING discovery_id")
                .execute(Tuple.of(
                        name,
                        ip,
                        port,
                        credential_profiles,
                        status
                ), execute -> {
                    if (execute.succeeded())
                    {
                        RowSet<Row> rows = execute.result();
                        if (rows.size() > 0)
                        {
                            Long discoveryId = rows.iterator().next().getLong("discovery_id");
                            promise.complete(discoveryId);
                        }
                        else {
                            promise.fail("Some problem");
                        }
                    }
                    else {
                        promise.fail(execute.cause());
                    }
                });
        return promise.future();
    }

    public Future<JsonArray> getAll()
    {
        Promise<JsonArray> promise = Promise.promise();

            client.query("SELECT * FROM discoveries")
                    .execute(execute ->{
                        if(execute.succeeded())
                        {
                            RowSet<Row> rows = execute.result();

                            var discoveries = new JsonArray();

                            for(Row row : rows)
                            {
                                var discovery = new JsonObject()
                                        .put("discovery.id",row.getLong("discovery_id"))
                                        .put("discovery.credential.profile",row.getLong("credential_profile"))
                                        .put("discovery.name",row.getString("name"))
                                        .put("discovery.ip",row.getString("ip"))
                                        .put("discovery.port",row.getInteger("port"))
                                        .put("discovery.credential.profiles",row.getJsonArray("credential_profiles"))
                                        .put("discovery.status",row.getString("status"));

                                discoveries.add(discovery);
                            }
                            promise.complete(discoveries);
                        }
                        else
                        {
                            promise.fail(execute.cause());
                        }
                    });

        return promise.future();
    }

    //Query for fetching discovery
    public Future<JsonObject> get(Long discoveryID)
    {
        Promise<JsonObject> promise = Promise.promise();

        client.preparedQuery("SELECT * FROM discoveries WHERE discovery_id=$1")
                .execute(Tuple.of(discoveryID),execute->{
                    if(execute.succeeded())
                    {
                        RowSet<Row> rows = execute.result();

                        if(rows.size()>0)
                        {
                            Row row = rows.iterator().next();
                            var response = new JsonObject()
                                    .put("discovery.id",row.getLong("discovery_id"))
                                    .put("discovery.credential.profile",row.getLong("credential_profile"))
                                    .put("discovery.name",row.getString("name"))
                                    .put("discovery.ip",row.getString("ip"))
                                    .put("discovery.port",row.getInteger("port"))
                                    .put("discovery.credential.profiles",row.getJsonArray("credential_profiles"))
                                    .put("discovery.status",row.getString("status"));

                            promise.complete(response);
                        }
                        else
                        {
                            promise.fail("Discovery not found for ID: "+discoveryID);
                        }
                    }
                    else
                    {
                        promise.fail(execute.cause());
                    }
                });
        return promise.future();
    }

    //Query to delete discovery
    public Future<Void> delete(Long discoveryID)
    {
        Promise<Void> promise = Promise.promise();

        client.preparedQuery("DELETE FROM discoveries WHERE discovery_id = $1")
                .execute(Tuple.of(discoveryID), execute ->{
                        if(execute.succeeded())
                        {
                            if(execute.result().rowCount()>0)
                            {
                                promise.complete();
                            }
                            else
                            {
                                promise.fail("No record found with the provided long_id");
                            }
                        }
                        else
                        {
                            promise.fail(execute.cause());
                        }
                    });

        return promise.future();
    }

    //Query to update discovery
    public Future<Void> update(Long discoveryId, String name, String ip, Integer port, JsonArray credential_profiles)
    {
        Promise<Void> promise = Promise.promise();

        client.preparedQuery("UPDATE discoveries " +
                        "SET name = $1, " +
                        "    ip = $2, " +
                        "    port = $3, " +
                        "    credential_profiles = $4 " +
                        "WHERE discovery_id = $5")
                .execute(Tuple.of(
                        name,
                        ip,
                        port,
                        credential_profiles,
                        discoveryId
                ), execute -> {
                    if (execute.succeeded())
                    {
                        if (execute.result().rowCount() > 0)
                        {
                            promise.complete(); // Update succeeded
                        }
                        else
                        {
                            promise.fail("Discovery not found for ID: " + discoveryId);
                        }
                    }
                    else
                    {
                        promise.fail(execute.cause());
                    }
                });

        return promise.future();
    }

    public Future<JsonObject> fetch(Long discoveryID)
    {
    Promise<JsonObject> promise = Promise.promise();

    client.preparedQuery(
                    """
                            SELECT d.ip, d.port,
                                   json_agg(
                                       json_build_object(
                                           'profile.id', c.profile_id,
                                           'profile.protocol', c.profile_protocol,
                                           'user.name', c.user_name,
                                           'user.password', c.user_password,
                                           'community', c.community,
                                           'version', c.version
                                       )
                                   ) AS credential_details
                            FROM discoveries d
                            LEFT JOIN LATERAL jsonb_array_elements_text(d.credential_profiles) AS elem(profile_id_text) ON TRUE
                            LEFT JOIN credentials c
                                ON c.profile_id = elem.profile_id_text::bigint
                            WHERE d.discovery_id = $1
                            GROUP BY d.ip, d.port;
                            """)
            .execute(Tuple.of(discoveryID), execute -> {
                if (execute.succeeded())
                {
                    RowSet<Row> rows = execute.result();

                    if (rows.iterator().hasNext())
                    {
                        Row row = rows.iterator().next();

                        var response = new JsonObject()
                                .put("discovery.ip", row.getString("ip"))
                                .put("discovery.port", row.getInteger("port"))
                                .put("discovery.credential.profiles", row.getJsonArray("credential_details"));

                        promise.complete(response);
                    }
                    else
                    {
                        promise.fail("No record found for this ID, please enter a valid ID");
                    }
                }
                else
                {
                    promise.fail("Failed to execute query to discover the device: " + execute.cause().getMessage());
                }
            });

    return promise.future();
}


    public Future<JsonObject> fetchProfile(Long profileID)
    {
        Promise<JsonObject> promise = Promise.promise();

        client.preparedQuery("SELECT profile_protocol,user_name,user_password,community,version FROM credentials WHERE profile_id = $1")
                .execute(Tuple.of(profileID), result -> {
            if (result.succeeded())
            {
                if(result.result().size() > 0)
                {
                    Row row = result.result().iterator().next();

                    // Construct a JsonObject based on the protocol
                    var profileData = new JsonObject()
                            .put("credential.protocol", row.getString("profile_protocol"))
                            .put("user.name", row.getString("user_name"))
                            .put("user.password", row.getString("user_password"))
                            .put("community", row.getString("community"))
                            .put("version", row.getString("version"));

                    promise.complete(profileData);
                }
                else
                {
                    promise.fail("No profile found for ID: " + profileID);
                }
            }
            else
            {
                promise.fail("Database query failed: " + result.cause().getMessage());
            }
        });
        return promise.future();
    }

    public Future<Boolean> updateStatus(Long profileID, String status, String hostname, Long discoveryID)
    {
        Promise<Boolean> promise = Promise.promise();
        try
        {
            client.preparedQuery("UPDATE discoveries SET credential_profile = $1 , status = $2 , hostname = $3 WHERE discovery_id = $4")
                    .execute(Tuple.of(profileID,status,hostname,discoveryID), result->{
                        if(result.succeeded())
                        {
                            promise.complete(true);
                        }
                        else
                        {
                            promise.fail("Failed to execute query : "+result.cause().getMessage());
                        }
                    });
        }
        catch (Exception exception)
        {
            promise.fail("Error occurred while fetching ip address and port number : " + exception.getMessage());
        }
        return promise.future();
    }


}
