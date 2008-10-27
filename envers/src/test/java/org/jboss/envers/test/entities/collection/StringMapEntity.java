package org.jboss.envers.test.entities.collection;

import org.jboss.envers.Versioned;
import org.hibernate.annotations.CollectionOfElements;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import java.util.Map;
import java.util.HashMap;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class StringMapEntity {
    @Id
    @GeneratedValue
    private Integer id;

    @Versioned
    @CollectionOfElements
    private Map<String, String> strings;

    public StringMapEntity() {
        strings = new HashMap<String, String>();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Map<String, String> getStrings() {
        return strings;
    }

    public void setStrings(Map<String, String> strings) {
        this.strings = strings;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StringMapEntity)) return false;

        StringMapEntity that = (StringMapEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }

    public String toString() {
        return "SME(id = " + id + ", strings = " + strings + ")";
    }
}