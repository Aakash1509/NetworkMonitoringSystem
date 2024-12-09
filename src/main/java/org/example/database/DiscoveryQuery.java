package org.example.database;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.example.model.Credential;
import org.example.model.Discoveries;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static org.example.Bootstrap.client;

public class DiscoveryQuery
{
//    private final  client;

//    public DiscoveryQuery(SqlClient sqlClient)
//    {
//        this.client = sqlClient;
//    }

    //Query for creating discovery
    public Future<Long> insert(Discoveries discovery)
    {
        Promise<Long> promise = Promise.promise();

        String sql = "INSERT INTO discoveries (name, ip, port, credential_profiles, status) " +
                "VALUES ($1, $2, $3, $4, $5) RETURNING discovery_id";

        client.preparedQuery(sql)
                .execute(Tuple.of(
                        discovery.name(),
                        discovery.ip(),
                        discovery.port(),
                        discovery.credential_profiles(),
                        discovery.status()
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

    public Future<List<Discoveries>> getAll()
    {
        Promise<List<Discoveries>> promise = Promise.promise();

            client.query("SELECT * FROM discoveries")
                    .execute(result->{
                        if(result.succeeded())
                        {
                            RowSet<Row> rows = result.result();

                            List<Discoveries> discoveries = new ArrayList<>();

                            for(Row row : rows)
                            {
                                Discoveries discovery = new Discoveries(
                                        row.getLong("discovery_id"),
                                        row.getLong("credential_profile"),
                                        row.getString("name"),
                                        row.getString("ip"),
                                        row.getInteger("port"),
                                        row.getJsonArray("credential_profiles"),
                                        row.getString("status")
                                );
                                discoveries.add(discovery);
                            }
                            promise.complete(discoveries);
                        }
                        else
                        {
                            promise.fail("Failed to fetch discoveries : "+result.cause().getMessage());
                        }
                    });

        return promise.future();
    }

    public Future<Discoveries> get(long discoveryID)
    {
        Promise<Discoveries> promise = Promise.promise();

        String sql = "SELECT * FROM discoveries WHERE discovery_id=$1";

        client.preparedQuery(sql)
                .execute(Tuple.of(discoveryID),result->{
                    if(result.succeeded())
                    {
                        RowSet<Row> rows = result.result();

                        if(rows.size()>0)
                        {
                            Row row = rows.iterator().next();

                            Discoveries discovery = new Discoveries(
                                    row.getLong("discovery_id"),
                                    row.getLong("credential_profile"),
                                    row.getString("name"),
                                    row.getString("ip"),
                                    row.getInteger("port"),
                                    row.getJsonArray("credential_profiles"),
                                    row.getString("status")
                            );
                            promise.complete(discovery);
                        }
                        else
                        {
                            promise.fail("Discovery not found for ID: "+discoveryID);
                        }
                    }
                    else
                    {
                        promise.fail(result.cause());
                    }
                });
        return promise.future();
    }

    public Future<Void> delete(long discoveryID)
    {
        Promise<Void> promise = Promise.promise();

        client.preparedQuery("DELETE FROM discoveries WHERE discovery_id = $1")
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

    public Future<JsonObject> fetch(Long id)
    {
        Promise<JsonObject> promise = Promise.promise();
        try
        {
            client.preparedQuery("SELECT ip_address,port_number FROM discoveries WHERE long_id = ?")
                    .execute(Tuple.of(id),result->{
                       if(result.succeeded())
                       {
                            RowSet<Row> rows = result.result();

                            if(rows.iterator().hasNext())
                            {
                                Row row = rows.iterator().next();
                                promise.complete(new JsonObject()
                                        .put("ip_address", row.getString("ip_address"))
                                        .put("port_number", row.getInteger("port_number")));
                            }
                            else
                            {
                                promise.fail("No record found for this ID, please enter a valid ID");
                            }
                       }
                       else
                       {
                           promise.fail("Failed to execute query to fetch ip and port address : "+result.cause().getMessage());
                       }
                    });
        }
        catch (Exception exception)
        {
            promise.fail("Error occurred while fetching ip address and port number : " + exception.getMessage());
        }
        return promise.future();

    }

    public Future<Boolean> updateStatus(Long id, String status, Timestamp timestamp)
    {
        Promise<Boolean> promise = Promise.promise();

        try
        {
            client.preparedQuery("UPDATE discoveries SET status = ? , last_checked = ? WHERE long_id = ?")
                    .execute(Tuple.of(status,timestamp,id),result->{
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
