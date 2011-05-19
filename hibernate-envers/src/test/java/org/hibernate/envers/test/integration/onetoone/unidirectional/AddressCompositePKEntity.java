package org.hibernate.envers.test.integration.onetoone.unidirectional;

import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

/**
 * @author Andrei Zagorneanu
 */
@Entity
@Audited
@IdClass(AddressPK.class)
public class AddressCompositePKEntity {
    private Integer id;
    private Integer ver;
    private String street;

    public AddressCompositePKEntity() {
    }

    public AddressCompositePKEntity(Integer id, Integer ver, String street) {
        this.setId(id);
        this.setVer(ver);
        this.setStreet(street);
    }

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Id
    @Column(name = "VER", nullable = false, updatable = false)
    public Integer getVer() {
        return ver;
    }

    public void setVer(Integer ver) {
        this.ver = ver;
    }

    @Column(name = "STREET", nullable = false)
    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String toString() {
        return "AddressCompositePKEntity[id = " + getId() + ", ver = " + getVer() + ", street = " + getStreet() + "]";
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + ((id == null) ? 0 : id.hashCode());
        result = 31 * result + ((ver == null) ? 0 : ver.hashCode());
        result = 31 * result + ((street == null) ? 0 : street.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AddressCompositePKEntity)) return false;

        AddressCompositePKEntity that = (AddressCompositePKEntity) o;

        if (ver != null ? !ver.equals(that.ver) : that.ver != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (street != null ? !street.equals(that.street) : that.street != null) return false;

        return true;
    }
}
