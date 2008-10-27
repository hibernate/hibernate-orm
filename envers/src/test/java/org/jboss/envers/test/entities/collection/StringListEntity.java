package org.jboss.envers.test.entities.collection;

import org.jboss.envers.Versioned;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.IndexColumn;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class StringListEntity {
    @Id
    @GeneratedValue
    private Integer id;

    @Versioned
    @CollectionOfElements
    @IndexColumn(name = "list_index")
    private List<String> strings;

    public StringListEntity() {
        strings = new ArrayList<String>();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<String> getStrings() {
        return strings;
    }

    public void setStrings(List<String> strings) {
        this.strings = strings;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StringListEntity)) return false;

        StringListEntity that = (StringListEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }

    public String toString() {
        return "SLE(id = " + id + ", strings = " + strings + ")";
    }
}