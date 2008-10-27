package org.jboss.envers.test.entities;

import org.jboss.envers.Versioned;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class IntTestEntity {
    @Id
    @GeneratedValue
    private Integer id;

    @Versioned
    private Integer number;

    public IntTestEntity() {
    }

    public IntTestEntity(Integer number, Integer id) {
        this.id = id;
        this.number = number;
    }

    public IntTestEntity(Integer number) {
        this.number = number;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IntTestEntity)) return false;

        IntTestEntity that = (IntTestEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (number != null ? !number.equals(that.number) : that.number != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (number != null ? number.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "ITE(id = " + id + ", number = " + number + ")";
    }
}