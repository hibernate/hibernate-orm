package org.jboss.envers.test.entities;

import org.jboss.envers.Versioned;
import org.jboss.envers.Unversioned;

import javax.persistence.Id;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Versioned
public class UnversionedEntity {
    @Id
    @GeneratedValue
    private Integer id;

    @Basic
    private String data1;

    @Basic
    @Unversioned
    private String data2;

    public UnversionedEntity() {
    }

    public UnversionedEntity(String data1, String data2) {
        this.data1 = data1;
        this.data2 = data2;
    }

    public UnversionedEntity(Integer id, String data1, String data2) {
        this.id = id;
        this.data1 = data1;
        this.data2 = data2;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getData1() {
        return data1;
    }

    public void setData1(String data1) {
        this.data1 = data1;
    }

    public String getData2() {
        return data2;
    }

    public void setData2(String data2) {
        this.data2 = data2;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UnversionedEntity)) return false;

        UnversionedEntity that = (UnversionedEntity) o;

        if (data1 != null ? !data1.equals(that.data1) : that.data1 != null) return false;
        if (data2 != null ? !data2.equals(that.data2) : that.data2 != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (data1 != null ? data1.hashCode() : 0);
        result = 31 * result + (data2 != null ? data2.hashCode() : 0);
        return result;
    }
}
