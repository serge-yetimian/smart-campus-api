package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // GET /sensors/{sensorId}/readings — get all readings for this sensor
    @GET
    public Response getReadings() {
        if (!DataStore.sensors.containsKey(sensorId)) {
            return Response.status(404)
                .entity("{\"error\":\"Sensor not found\"}")
                .build();
        }

        List<SensorReading> sensorReadings = DataStore.readings.getOrDefault(
            sensorId, new ArrayList<>()
        );
        return Response.ok(sensorReadings).build();
    }

    // POST /sensors/{sensorId}/readings — add a new reading
    @POST
    public Response addReading(SensorReading reading) {
        if (!DataStore.sensors.containsKey(sensorId)) {
            return Response.status(404)
                .entity("{\"error\":\"Sensor not found\"}")
                .build();
        }

        // Block readings if sensor is in MAINTENANCE
        String status = DataStore.sensors.get(sensorId).getStatus();
        if ("MAINTENANCE".equalsIgnoreCase(status)) {
            throw new SensorUnavailableException(sensorId);
        }

        // Create the reading with auto-generated ID and timestamp
        SensorReading newReading = new SensorReading(reading.getValue());

        // Save the reading
        DataStore.readings.get(sensorId).add(newReading);

        // Update the parent sensor's currentValue
        DataStore.sensors.get(sensorId).setCurrentValue(newReading.getValue());

        return Response.status(201).entity(newReading).build();
    }
}