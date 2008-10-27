package org.jboss.envers.test.integration.onetoone.unidirectional;

import org.jboss.envers.Versioned;

import javax.persistence.Id;
import javax.persistence.Entity;

/**
 * Unidirectional ReferencEd Entity
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class UniRefEdEntity {
    @Id
    private Integer id;

    @Versioned
    private String data;

    public UniRefEdEntity() {
    }

    public UniRefEdEntity(Integer id, String data) {
        this.id = id;
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UniRefEdEntity)) return false;

        UniRefEdEntity that = (UniRefEdEntity) o;

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
}
