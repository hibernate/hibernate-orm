package org.jboss.envers.test.integration.naming;

import org.jboss.envers.Versioned;
import org.jboss.envers.VersionsTable;

import javax.persistence.*;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name="naming_test_entity_1")
@VersionsTable("naming_test_entity_1_versions")
public class NamingTestEntity1 {
    @Id
    @GeneratedValue
    @Column(name = "nte_id")
    private Integer id;

    @Column(name = "nte_data")
    @Versioned
    private String data;

    public NamingTestEntity1() {
    }

    public NamingTestEntity1(String data) {
        this.data = data;
    }

    public NamingTestEntity1(Integer id, String data) {
        this.id = id;
        this.data = data;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NamingTestEntity1)) return false;

        NamingTestEntity1 that = (NamingTestEntity1) o;

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
