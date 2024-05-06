package org.hibernate.orm.test.annotations.cid;

import jakarta.persistence.*;

@Entity
@IdClass(FlightSegmentConfigurationId.class)
@Table(name = "flight_segment_configuration")
public class FlightSegmentConfiguration {

    @Id
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", referencedColumnName = "flight_id", nullable = false)
    @JoinColumn(name = "segment_number", referencedColumnName = "segment_number", nullable = false)
    private FlightSegment segment;

    public FlightSegment getSegment() {
        return segment;
    }

    public void setSegment(FlightSegment segment) {
        this.segment = segment;
    }

}
