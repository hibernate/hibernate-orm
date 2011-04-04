package org.hibernate.envers.test.entities.index;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Table;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * @author Lukasz Antoniak (lukasz.antoniak at gmail dot com)
 */
@Entity
@Table(appliesTo = "MultiColumnIndexEntity", indexes = { @Index(name="idx1", columnNames = { "id", "data1" } ) } )
public class MultiColumnIndexEntity {
    @Id
    @GeneratedValue
    private Long id;

    @Audited
    @Index(name = "idx2")
    private String data1;

    @Audited
    @Index(name = "idx2")
    private String data2;

    @Audited
    @ManyToOne
    private SingleColumnIndexEntity parent;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public SingleColumnIndexEntity getParent() {
        return parent;
    }

    public void setParent(SingleColumnIndexEntity parent) {
        this.parent = parent;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MultiColumnIndexEntity)) return false;

        MultiColumnIndexEntity that = (MultiColumnIndexEntity) o;

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

    public String toString() {
        return "MultiColumnIndexEntity(id = " + id + ", data1 = " + data1 + ", data2 = " + data2 + ")";
    }
}
