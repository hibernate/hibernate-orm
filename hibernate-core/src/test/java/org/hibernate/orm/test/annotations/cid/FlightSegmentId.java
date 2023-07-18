package org.hibernate.orm.test.annotations.cid;

import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

public class FlightSegmentId implements Serializable {

    private Integer flight;
    private Integer segmentNumber;

    public FlightSegmentId() {
    }

    public FlightSegmentId(Integer flight, Integer segmentNumber) {
        this.flight = flight;
        this.segmentNumber = segmentNumber;
    }

    public Integer getFlight() {
        return flight;
    }

    public void setFlight(Integer flight) {
        this.flight = flight;
    }

    public Integer getSegmentNumber() {
        return segmentNumber;
    }

    public void setSegmentNumber(Integer segmentNumber) {
        this.segmentNumber = segmentNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlightSegmentId that = (FlightSegmentId) o;
        return Objects.equals(flight, that.flight) && Objects.equals(segmentNumber, that.segmentNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flight, segmentNumber);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FlightSegmentId.class.getSimpleName() + "[", "]")
                .add("flight=" + flight)
                .add("segmentNumber=" + segmentNumber)
                .toString();
    }

}
