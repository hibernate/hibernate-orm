package org.hibernate.orm.test.annotations.cid;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "flight")
public class Flight {

    @Id
    @Column(name = "id")
    private Integer id;

    @OneToMany(mappedBy = "flight", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FlightSegment> segments;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<FlightSegment> getSegments() {
        return segments;
    }

    public void setSegments(List<FlightSegment> segments) {
        this.segments = segments;
    }

    public void addSegment(FlightSegment segment) {
        segment.setFlight(this);
        segments.add(segment);
    }

}
