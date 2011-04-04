package org.hibernate.envers.test.entities.index;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Table;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.Set;

/**
 * @author Lukasz Antoniak (lukasz.antoniak at gmail dot com)
 */
@Entity
@Table(appliesTo = "SingleColumnIndexEntity", indexes = { @Index(name="idx1", columnNames = { "id" } ), @Index(name="idx2", columnNames = { "data" } ) } )
public class SingleColumnIndexEntity {
    @Id
    @GeneratedValue
    private Long id;

    @Audited
    @Index(name = "idx3")
    private String data;

    @Audited
    @Index(name = "idx4")
    @OneToMany(mappedBy = "parent")
    private Set<MultiColumnIndexEntity> children;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Set<MultiColumnIndexEntity> getChildren() {
        return children;
    }

    public void setChildren(Set<MultiColumnIndexEntity> children) {
        this.children = children;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SingleColumnIndexEntity)) return false;

        SingleColumnIndexEntity that = (SingleColumnIndexEntity) o;

        if (data != null ? !data.equals(that.data) : that.data != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "SingleColumnIndexEntity(id = " + id + ", data = " + data + ")";
    }
}
