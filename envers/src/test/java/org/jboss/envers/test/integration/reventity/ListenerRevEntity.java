package org.jboss.envers.test.integration.reventity;

import org.jboss.envers.RevisionNumber;
import org.jboss.envers.RevisionTimestamp;
import org.jboss.envers.RevisionEntity;

import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Entity;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@RevisionEntity(TestRevisionListener.class)
public class ListenerRevEntity {
    @Id
    @GeneratedValue
    @RevisionNumber
    private int id;

    @RevisionTimestamp
    private long timestamp;

    private String data;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ListenerRevEntity)) return false;

        ListenerRevEntity revEntity = (ListenerRevEntity) o;

        if (id != revEntity.id) return false;
        if (timestamp != revEntity.timestamp) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = id;
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }
}
