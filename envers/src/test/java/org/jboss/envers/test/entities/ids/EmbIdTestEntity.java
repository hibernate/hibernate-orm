package org.jboss.envers.test.entities.ids;

import org.jboss.envers.Versioned;
import org.jboss.envers.test.entities.ids.EmbId;

import javax.persistence.Entity;
import javax.persistence.EmbeddedId;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class EmbIdTestEntity {
    @EmbeddedId
    private EmbId id;

    @Versioned
    private String str1;

    public EmbIdTestEntity() {
    }

    public EmbIdTestEntity(String str1) {
        this.str1 = str1;
    }

    public EmbIdTestEntity(EmbId id, String str1) {
        this.id = id;
        this.str1 = str1;
    }

    public EmbId getId() {
        return id;
    }

    public void setId(EmbId id) {
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
        if (!(o instanceof EmbIdTestEntity)) return false;

        EmbIdTestEntity that = (EmbIdTestEntity) o;

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
