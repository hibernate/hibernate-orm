package org.jboss.envers.test.integration.data;

import org.jboss.envers.Versioned;

import javax.persistence.*;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Versioned
public class EnumTestEntity {
    @Id
    @GeneratedValue
    private Integer id;

    public enum E1 { X, Y }
    public enum E2 { A, B }

    @Enumerated(EnumType.STRING)
    private E1 enum1;

    @Enumerated(EnumType.ORDINAL)
    private E2 enum2;

    public EnumTestEntity() {
    }

    public EnumTestEntity(E1 enum1, E2 enum2) {
        this.enum1 = enum1;
        this.enum2 = enum2;
    }

    public EnumTestEntity(Integer id, E1 enum1, E2 enum2) {
        this.id = id;
        this.enum1 = enum1;
        this.enum2 = enum2;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public E1 getEnum1() {
        return enum1;
    }

    public void setEnum1(E1 enum1) {
        this.enum1 = enum1;
    }

    public E2 getEnum2() {
        return enum2;
    }

    public void setEnum2(E2 enum2) {
        this.enum2 = enum2;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EnumTestEntity)) return false;

        EnumTestEntity that = (EnumTestEntity) o;

        if (enum1 != that.enum1) return false;
        if (enum2 != that.enum2) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (enum1 != null ? enum1.hashCode() : 0);
        result = 31 * result + (enum2 != null ? enum2.hashCode() : 0);
        return result;
    }
}
