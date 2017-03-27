package org.hibernate.query.criteria.internal.expression;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Event {

    @Id
    private Long id;

    @Column
    private EventType type;

    protected Event() {
    }

    public EventType getType() {
        return type;
    }

    public Event type(EventType type) {
        this.type = type;
        return this;
    }
}
