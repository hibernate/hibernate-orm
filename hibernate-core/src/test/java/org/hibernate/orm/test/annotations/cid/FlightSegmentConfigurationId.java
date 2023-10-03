package org.hibernate.orm.test.annotations.cid;

import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

public class FlightSegmentConfigurationId implements Serializable {

    private FlightSegmentId segment;

    public FlightSegmentConfigurationId() {
    }

    public FlightSegmentConfigurationId(FlightSegmentId segment) {
        this.segment = segment;
    }

    public FlightSegmentId getSegment() {
        return segment;
    }

    public void setSegment(FlightSegmentId segment) {
        this.segment = segment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlightSegmentConfigurationId that = (FlightSegmentConfigurationId) o;
        return Objects.equals(segment, that.segment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(segment);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FlightSegmentConfigurationId.class.getSimpleName() + "[", "]")
                .add("segment=" + segment)
                .toString();
    }

}
