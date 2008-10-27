package org.jboss.envers.test.integration.data;

import org.jboss.envers.Versioned;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import java.util.Date;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class DateTestEntity {
    @Id
    @GeneratedValue
    private Integer id;

    @Versioned
    private Date date;

    public DateTestEntity() {
    }

    public DateTestEntity(Date date) {
        this.date = date;
    }

    public DateTestEntity(Integer id, Date date) {
        this.id = id;
        this.date = date;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DateTestEntity)) return false;

        DateTestEntity that = (DateTestEntity) o;

        if (date != null) {
            if (that.date == null) {
                return false;
            }

            if (date.getTime() != that.date.getTime()) {
                return false;
            }
        }
        
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        return result;
    }
}