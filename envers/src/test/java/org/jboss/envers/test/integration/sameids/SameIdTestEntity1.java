package org.jboss.envers.test.integration.sameids;

import org.jboss.envers.Versioned;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class SameIdTestEntity1 {
    @Id
    private Integer id;

    @Versioned
    private String str1;

    public SameIdTestEntity1() {
    }

    public SameIdTestEntity1(String str1) {
        this.str1 = str1;
    }

    public SameIdTestEntity1(Integer id, String str1) {
        this.id = id;
        this.str1 = str1;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getStr1() {
        return str1;
    }

    public void setStr1(String str1) {
        this.str1 = str1;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SameIdTestEntity1)) return false;

        SameIdTestEntity1 that = (SameIdTestEntity1) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (str1 != null ? !str1.equals(that.str1) : that.str1 != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (str1 != null ? str1.hashCode() : 0);
        return result;
    }
}