package org.jboss.envers.test.integration.superclass;

import org.jboss.envers.Versioned;

import javax.persistence.MappedSuperclass;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@MappedSuperclass
public class SuperclassOfEntity {
    @Versioned
    private String str;

    public SuperclassOfEntity() {
    }

    public SuperclassOfEntity(String str) {
        this.str = str;
    }

    public String getStr() {
        return str;
    }

    public void setStr(String str) {
        this.str = str;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SuperclassOfEntity)) return false;

        SuperclassOfEntity that = (SuperclassOfEntity) o;

        if (str != null ? !str.equals(that.str) : that.str != null) return false;

        return true;
    }

    public int hashCode() {
        return (str != null ? str.hashCode() : 0);
    }
}
