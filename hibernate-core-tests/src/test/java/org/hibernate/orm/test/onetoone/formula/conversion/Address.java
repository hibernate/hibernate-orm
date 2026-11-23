package org.hibernate.orm.test.onetoone.formula.conversion;
import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Check;

/// Formula mapping conversion fixture.
///
/// @author Steve Ebersole
@Entity(name = "Address")
@IdClass(AddressId.class)
@BatchSize(size = 2)
@Check(constraints = "addressType in ('MAILING', 'HOME', 'BUSINESS')")
public class Address implements Serializable {
	@Id
	@ManyToOne(optional = false)
	@JoinColumn(name = "personName")
	private Person person;
	@Id
	@Column(name = "addressType")
	private String type;
	private String street;
	private String state;
	private String zip;

	public Person getPerson() {
		return person;
	}
	public void setPerson(Person person) {
		this.person = person;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getStreet() {
		return street;
	}
	public void setStreet(String street) {
		this.street = street;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getZip() {
		return zip;
	}
	public void setZip(String zip) {
		this.zip = zip;
	}

	public boolean equals(Object that) {
		if ( !(that instanceof Address) ) return false;
		Address address = (Address) that;
		return address.getType().equals(type) &&
			address.getPerson().getName().equals( person.getName() );
	}

	public int hashCode() {
		return person.getName().hashCode() + type.hashCode();
	}
}
