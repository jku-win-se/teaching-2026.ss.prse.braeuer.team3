package at.jku.se.smarthome.dto;

import java.util.List;

/**
 * Response body for a completed day simulation run (US-020).
 *
 * <p>Contains an ordered list of device state changes produced by the simulation
 * engine, sorted chronologically by simulated time. The list is empty when no
 * automation rules fire during the simulated day.</p>
 */
public class SimulationResponse {

    /** Ordered list of device state changes produced during the simulation. */
    private List<SimulationEvent> events;

    /** Default no-arg constructor required for JSON serialization. */
    public SimulationResponse() {
    }

    /**
     * Constructs a response with the given event list.
     *
     * @param events ordered list of simulation events
     */
    public SimulationResponse(List<SimulationEvent> events) {
        this.events = events;
    }

    /**
     * Returns the ordered list of simulation events.
     *
     * @return list of {@link SimulationEvent}
     */
    public List<SimulationEvent> getEvents() {
        return events;
    }

    /**
     * Sets the ordered list of simulation events.
     *
     * @param events list of {@link SimulationEvent}
     */
    public void setEvents(List<SimulationEvent> events) {
        this.events = events;
    }
}
