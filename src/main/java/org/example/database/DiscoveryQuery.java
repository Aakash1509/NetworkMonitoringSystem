package org.example.database;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

public class DiscoveryQuery
{
    private final SqlClient client;

    public DiscoveryQuery(SqlClient sqlClient)
    {
        this.client = sqlClient;
    }

    public Future<Void> insert(String discoveryName, String ip, int port, long discoveryID)
    {
        Promise<Void> promise = Promise.promise();

        client.preparedQuery("INSERT INTO discoveries (discovery_name,ip_address,port_number,long_id) VALUES(?,?,?,?)")
                .execute(Tuple.of(discoveryName,ip,port,discoveryID), result->{
                    if(result.succeeded())
                    {
                        promise.complete();
                    }
                    else
                    {
                        promise.fail("Failed to insert discovery in database");
                    }
                });
        return promise.future();
    }

    public Future<JsonArray> getAllDiscoveries()
    {
        Promise<JsonArray> promise = Promise.promise();
        try
        {
            client.query("SELECT * FROM discoveries")
                    .execute(result->{
                        if(result.succeeded())
                        {
                            var discoveryArray = new JsonArray();

                            result.result().forEach(row -> {
                                var discovery = new JsonObject()
                                        .put("discovery.name",row.getString("discovery_name"))
                                        .put("discovery.ip",row.getString("ip_address"))
                                        .put("discovery.port",row.getInteger("port_number"))
                                        .put("discovery.id",row.getLong("long_id"));

                                discoveryArray.add(discovery);
                            });
                            promise.complete(discoveryArray);
                        }
                        else
                        {
                            promise.fail("Failed to fetch discoveries : "+result.cause().getMessage());
                        }
                    });
        }
        catch (Exception exception)
        {
            System.out.println("Error occurred : "+exception.getMessage());
        }
        return promise.future();
    }

    public Future<JsonObject> getDiscovery(long discoveryID)
    {
        Promise<JsonObject> promise = Promise.promise();

        try
        {
            client.preparedQuery("SELECT discovery_name,ip_address,port_number FROM discoveries WHERE long_id = ?")
                    .execute(Tuple.of(discoveryID),result->{
                        if(result.succeeded())
                        {
                            RowSet<Row> tuples = result.result();

                            if(tuples.size()>0)
                            {
                                Row row = tuples.iterator().next();

                                var discovery = new JsonObject()
                                        .put("discovery.name", row.getString("discovery_name"))
                                        .put("discovery.ip", row.getString("ip_address"))
                                        .put("discovery.port", row.getInteger("port_number"));

                                promise.complete(discovery);
                            }
                            else
                            {
                                promise.complete(new JsonObject().put("Error","No discovery found with this ID"));
                            }
                        }
                        else
                        {
                            promise.fail("Failed to fetch discovery : "+result.cause().getMessage());
                        }
                    });
        }
        catch (Exception exception)
        {
            promise.fail("Error occurred while attempting to fetch discovery: " + exception.getMessage());
        }
        return promise.future();
    }

    public Future<Void> deleteDiscovery(long discoveryID)
    {
        Promise<Void> promise = Promise.promise();

        try
        {
            client.preparedQuery("DELETE FROM discoveries WHERE long_id = ?")
                    .execute(Tuple.of(discoveryID),result->{
                        if(result.succeeded())
                        {
                            if(result.result().rowCount()>0)
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
                            promise.fail("Failed to execute delete query: "+result.cause().getMessage());
                        }
                    });
        }
        catch (Exception exception)
        {
            promise.fail("Error occurred while attempting to delete: " + exception.getMessage());
        }
        return promise.future();
    }

    public Future<Void> updateDiscovery(String discoveryName, String ip, int port, long discoveryID)
    {
        Promise<Void> promise = Promise.promise();

        try
        {
            client.preparedQuery("UPDATE discoveries SET discovery_name = ?, ip_address = ? , port_number = ? WHERE long_id = ?")
                    .execute(Tuple.of(discoveryName,ip,port,discoveryID),result->{
                        if(result.succeeded())
                        {
                            if(result.result().rowCount()>0)
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
                            promise.fail("Failed to execute update query: "+result.cause().getMessage());
                        }
                    });
        }
        catch (Exception exception)
        {
            promise.fail("Error occurred while attempting to update: " + exception.getMessage());
        }

        return promise.future();
    }

}
