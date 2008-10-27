package org.jboss.envers.test.entities;

import org.jboss.envers.Versioned;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class UnversionedStrTestEntity {
    @Id
    @GeneratedValue
    private Integer id;

    private String str;

    public UnversionedStrTestEntity() {
    }

    public UnversionedStrTestEntity(String str, Integer id) {
        this.str = str;
        this.id = id;
    }

    public UnversionedStrTestEntity(String str) {
        this.str = str;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getStr() {
        return str;
    }

    public void setStr(String str) {
        this.str = str;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UnversionedStrTestEntity)) return false;

        UnversionedStrTestEntity that = (UnversionedStrTestEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (str != null ? !str.equals(that.str) : that.str != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (str != null ? str.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "USTE(id = " + id + ", str = " + str + ")";
    }
}