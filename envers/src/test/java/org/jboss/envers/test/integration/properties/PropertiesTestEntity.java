package org.jboss.envers.test.integration.properties;

import org.jboss.envers.Versioned;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class PropertiesTestEntity {
    @Id
    @GeneratedValue
    private Integer id;

    @Versioned
    private String str;

    public PropertiesTestEntity() {
    }

    public PropertiesTestEntity(String str) {
        this.str = str;
    }

    public PropertiesTestEntity(Integer id, String str) {
        this.id = id;
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
        if (!(o instanceof PropertiesTestEntity)) return false;

        PropertiesTestEntity that = (PropertiesTestEntity) o;

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
}