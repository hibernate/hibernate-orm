package org.jboss.envers.test.integration.basic;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class BasicTestEntity3 {
    @Id
    @GeneratedValue
    private Integer id;

    private String str1;

    private String str2;

    public BasicTestEntity3() {
    }

    public BasicTestEntity3(String str1, String str2) {
        this.str1 = str1;
        this.str2 = str2;
    }

    public BasicTestEntity3(Integer id, String str1, String str2) {
        this.id = id;
        this.str1 = str1;
        this.str2 = str2;
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

    public String getStr2() {
        return str2;
    }

    public void setStr2(String str2) {
        this.str2 = str2;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BasicTestEntity3)) return false;

        BasicTestEntity3 that = (BasicTestEntity3) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (str1 != null ? !str1.equals(that.str1) : that.str1 != null) return false;
        if (str2 != null ? !str2.equals(that.str2) : that.str2 != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (str1 != null ? str1.hashCode() : 0);
        result = 31 * result + (str2 != null ? str2.hashCode() : 0);
        return result;
    }
}