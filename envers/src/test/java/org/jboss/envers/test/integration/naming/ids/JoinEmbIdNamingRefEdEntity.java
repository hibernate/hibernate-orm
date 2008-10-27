package org.jboss.envers.test.integration.naming.ids;

import org.jboss.envers.Versioned;

import javax.persistence.*;
import java.util.List;

/**
 * ReferencEd entity
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class JoinEmbIdNamingRefEdEntity {
    @Id
    @GeneratedValue
    private EmbIdNaming id;

    @Versioned
    private String data;

    @Versioned
    @OneToMany(mappedBy="reference")
    private List<JoinEmbIdNamingRefIngEntity> reffering;

    public JoinEmbIdNamingRefEdEntity() {
    }

    public JoinEmbIdNamingRefEdEntity(EmbIdNaming id, String data) {
        this.id = id;
        this.data = data;
    }

    public JoinEmbIdNamingRefEdEntity(String data) {
        this.data = data;
    }

    public EmbIdNaming getId() {
        return id;
    }

    public void setId(EmbIdNaming id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public List<JoinEmbIdNamingRefIngEntity> getReffering() {
        return reffering;
    }

    public void setReffering(List<JoinEmbIdNamingRefIngEntity> reffering) {
        this.reffering = reffering;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JoinEmbIdNamingRefEdEntity)) return false;

        JoinEmbIdNamingRefEdEntity that = (JoinEmbIdNamingRefEdEntity) o;

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
        return "JoinEmbIdNamingRefEdEntity(id = " + id + ", data = " + data + ")";
    }
}