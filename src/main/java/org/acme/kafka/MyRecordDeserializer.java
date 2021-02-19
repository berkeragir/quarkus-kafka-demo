package org.acme.kafka;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class MyRecordDeserializer extends ObjectMapperDeserializer<MyRecord> {

	public MyRecordDeserializer() {
		super(MyRecord.class);
	}
    

}




