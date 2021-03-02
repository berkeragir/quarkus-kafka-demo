package org.acme.kafka;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.util.JSONPObject;

import org.jboss.logmanager.Logger;
import org.jboss.resteasy.annotations.SseElementType;
import org.reactivestreams.Publisher;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.Synchronized;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;

/**
 * A simple resource retrieving the "in-memory" "my-data-stream" and sending the items to a server sent event.
 */
@Path("/records")
public class RecordController {

    Logger logger = Logger.getLogger(RecordController.class.getName());

    private Random random = new Random();

    final MyCounters myCounters = new MyCounters(0, 0, 0, 0);

    private String randomPayload;

    public RecordController() {
        try {

            // Load the included random payload to be used by default in messages.

            InputStream inputStream = getClass().getResourceAsStream("/data/random.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                this.randomPayload = reader.lines()
                        .collect(Collectors.joining(System.lineSeparator()));

            logger.info("Will use the contents of random.json as payload:\n" + randomPayload);
		} catch (Exception e) {
            e.printStackTrace();
        
            randomPayload = "Small payload";
            logger.warning(randomPayload);
		}
    }
    
    @Inject 
    @Channel("my-record-topic") 
    @OnOverflow(value = OnOverflow.Strategy.UNBOUNDED_BUFFER)
    Emitter<MyRecord> recordEmitter;


    // @Inject
    // @Channel("my-data-stream")
    // Publisher<MyRecord> records;


    // @GET
    // @Path("/stream")
    // @Produces(MediaType.SERVER_SENT_EVENTS) // denotes that server side events (SSE) will be produced
    // @SseElementType(MediaType.APPLICATION_JSON) // denotes that the contained data, within this SSE, is just regular text/plain data
    // public Publisher<MyRecord> stream() {
    //     return records;
    // }

    /* 
     * Endpoint for triggering new messages for kafka
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void sendNewRecords(SendPayloadRequest request) {
        // Send #num new messages to kafka

        int numberOfMessages = request.getNum();

        logger.info(String.format("New request send %d messages to kafka...", numberOfMessages));

        int i = 0;

        String dataToSend = randomPayload;

        if(request.getPayload() != null) {
            dataToSend = request.getPayload().toString();

            // logger.info("Using the provided data in the request as the message payload:");
            // logger.info(dataToSend);
        }

        for(i=0; i < numberOfMessages; i++) {
            recordEmitter
                .send(new MyRecord("someId", dataToSend, random.nextInt(10000)))
                .whenComplete((success, failure) -> {
                    if (failure != null) {
                        myCounters.incrementFailedMessages();
                    } else {
                        myCounters.incrementProcessedMessages();
                    }
                });
        }

        logger.info(String.format("Finished with %d successful, %d failed message processing.", myCounters.getProcessedMessages(), myCounters.getFailedMessages()));
    }

    /* 
     * Endpoint for getting the # of successful and failed message submissions since last request.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getCurrentCounters() {
        String response = String.format("%d,%d,%d,%d\n", 
            myCounters.getProcessedMessages(), myCounters.getFailedMessages(), myCounters.getTotalProcessed(), myCounters.getTotalFailed());
        myCounters.reset();

        return response;
    }


    @Data
    @AllArgsConstructor
    public static class MyCounters {
        private int processedMessages;
        private int failedMessages;

        private int totalProcessed = 0;
        private int totalFailed = 0;

        @Synchronized
        public void incrementProcessedMessages() {
            this.processedMessages++;
            this.totalProcessed++;
        }

        @Synchronized
        public void incrementFailedMessages() {
            this.failedMessages++;
            this.totalFailed++;
        }

        public void reset() {
            this.processedMessages = 0;
            this.failedMessages = 0;
        }
    }    

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendPayloadRequest {
        private int num;
        private Map<String, Object> payload;
    }
}
