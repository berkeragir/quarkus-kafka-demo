package org.acme.kafka;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.annotation.ObjectIdGenerators.StringIdGenerator;

import org.jboss.logmanager.Logger;
import org.jboss.resteasy.annotations.SseElementType;
import org.reactivestreams.Publisher;

import lombok.AllArgsConstructor;
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

    final MyCounters myCounters = new MyCounters(0, 0);

    private String randomPayload;

    public RecordController() {
        try {
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
    @Channel("generated-price") 
    @OnOverflow(value = OnOverflow.Strategy.UNBOUNDED_BUFFER)
    Emitter<MyRecord> recordEmitter;


    @Inject
    @Channel("my-data-stream")
    Publisher<MyRecord> records;


    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS) // denotes that server side events (SSE) will be produced
    @SseElementType(MediaType.APPLICATION_JSON) // denotes that the contained data, within this SSE, is just regular text/plain data
    public Publisher<MyRecord> stream() {
        return records;
    }

    @POST
    public void sendNewRecords(@FormParam("num") int num) {
        // Send #num new messages to kafka

        logger.info(String.format("New request send %d messages to kafka...", num));

        int i = 0;

        myCounters.setProcessedMessages(0);
        myCounters.setFailedMessages(0);

        for(i=0; i<num; i++) {
            recordEmitter
                .send(new MyRecord("someId", "My Name", randomPayload, "This is a kafka object with a message! Hey. I also have some random number.", random.nextInt(10000)))
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

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getCurrentCounters() {
        return String.format("{ \n\t\"successful\": %d,\n\t\"failed\": %d\n}", myCounters.getProcessedMessages(), myCounters.getFailedMessages());
    }

    @Data
    @AllArgsConstructor
    public static class MyCounters {
        private int processedMessages;
        private int failedMessages;

        @Synchronized
        public void incrementProcessedMessages() {
            this.processedMessages++;
        }

        @Synchronized
        public void incrementFailedMessages() {
            this.failedMessages++;
        }
    }    
}
