package org.acme.kafka;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@RegisterForReflection
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyRecord {
    private String id;
    // private String name;
    private String payload;
    // private String message;
    private int myNumber;
}
