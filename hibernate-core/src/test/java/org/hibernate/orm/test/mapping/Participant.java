package org.hibernate.orm.test.mapping;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "PARTICIPANT")
public class Participant implements Serializable {
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private ParticipantId id;

    public ParticipantId getId() {
        return id;
    }

    public void setId(ParticipantId id) {
        this.id = id;
    }
}
