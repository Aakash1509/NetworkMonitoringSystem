package org.example.services;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import org.example.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import java.nio.file.Paths;

public class FileSender extends AbstractVerticle
{
    private static final Logger logger = LoggerFactory.getLogger(FileSender.class);

    private ZMQ.Socket socket;

    private ZContext context;

    public void start()
    {
        try
        {
            context = new ZContext();

            socket = context.createSocket(ZMQ.PUSH);

            socket.connect("tcp://" + Constants.IP + ":" + Constants.ZMQ_PORT);

            //Periodically processing files
            vertx.setPeriodic(Constants.DATABASE_INTERVAL,id->processFiles());
        }
        catch (Exception exception)
        {
            logger.error("Failed to initialize ZMQ context: {}", exception.getMessage(), exception);
        }
    }

    private void processFiles()
    {
        vertx.fileSystem().readDir(Constants.BASE_DIRECTORY,result->
        {
           if (result.succeeded())
           {
               var files = result.result();

               if(!files.isEmpty())
               {
                   logger.info("Found {} files", files.size());

                   files.forEach(this::sendFile);
               }
               else
               {
                   logger.info("Currently no polled data available");
               }
           }
           else
           {
               logger.error("Error occurred: {}", result.cause().getMessage());
           }
        });
    }

    private void sendFile(String filePath)
    {
        vertx.fileSystem().readFile(filePath)
                .compose(fileContent ->
                {
                    // Extract file name from path
                    var fileName = Paths.get(filePath).getFileName().toString();

                    var fileParts = fileContent.toString().split("----------------------\n");

                    for (var part : fileParts)
                    {
                        if(!part.trim().isEmpty())
                        {
                            var message = new JsonObject()
                                    .put("filename", fileName)
                                    .put("content", part);

                            boolean result = socket.send(message.encode(), 0);

                            if (!result)
                            {
                                logger.error("Failed to send message via ZMQ.");
                            }
                            else
                            {
                                logger.info("File part sent to ZMQ: {}", fileName);
                            }
                        }
                    }

                    //Will delete file after it has been sent
                    return vertx.fileSystem().delete(filePath);
                })
                .onSuccess(v ->
                {
                    logger.info("File deleted successfully.");
                })
                .onFailure(error ->
                {
                    logger.error("Error: {}", error.getMessage());
                });
    }

    public void stop()
    {
        if (socket != null)
        {
            socket.close();
        }
        if (context != null)
        {
            context.close();
        }
    }
}
