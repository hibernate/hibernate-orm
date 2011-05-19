package org.hibernate.envers.test.integration.onetoone.unidirectional;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.OneToOne;

import org.hibernate.envers.Audited;

/**
 * @author Andrei Zagorneanu
 */
@Entity
@Audited
public class PersonCompositeFKEntity {
	private Integer id;
	private String name;
	private AddressCompositePKEntity address;

	public PersonCompositeFKEntity() {
	}

	public PersonCompositeFKEntity(Integer id, String name, AddressCompositePKEntity address) {
		this.setId(id);
		this.setName(name);
		this.setAddress(address);
	}

	@Id
	@Column(name = "ID", nullable = false, updatable = false)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "NAME", nullable = false)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToOne(cascade = { CascadeType.ALL }, orphanRemoval = true)
	@JoinColumns({@JoinColumn(name = "ID_ADDRESS", referencedColumnName = "ID"),
                  @JoinColumn(name = "VER_ADDRESS", referencedColumnName = "VER")})
	public AddressCompositePKEntity getAddress() {
		return address;
	}

	public void setAddress(AddressCompositePKEntity address) {
		this.address = address;
	}

	public String toString() {
		return "PersonCompositeFKEntity[id = " + getId() + ", name = " + getName() + "]";
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + ((id == null) ? 0 : id.hashCode());
		result = 31 * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
        if (!(o instanceof PersonCompositeFKEntity)) return false;

        PersonCompositeFKEntity that = (PersonCompositeFKEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
	}
}
