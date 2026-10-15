package org.hibernate.orm.test.onetoone.formula.conversion;
import java.io.Serializable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.JoinColumn;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;

/// Shared-column and formula mapping conversion fixture.
///
/// @author Steve Ebersole
@Entity(name = "Person")
public class Person implements Serializable {
	@Id
	@Column(name = "name")
	private String name;
	@OneToOne(optional = true, cascade = CascadeType.ALL)
	@JoinColumnsOrFormulas({
		@JoinColumnOrFormula(column = @JoinColumn(name = "name", referencedColumnName = "personName")),
		@JoinColumnOrFormula(formula = @JoinFormula(value = "'HOME'", referencedColumnName = "addressType"))
	})
	private Address address;
	@OneToOne(optional = true)
	@JoinColumnsOrFormulas({
		@JoinColumnOrFormula(column = @JoinColumn(name = "name", referencedColumnName = "personName")),
		@JoinColumnOrFormula(formula = @JoinFormula(value = "'MAILING'", referencedColumnName = "addressType"))
	})
	private Address mailingAddress;

	public Address getAddress() {
		return address;
	}
	public void setAddress(Address address) {
		this.address = address;
	}
	public Address getMailingAddress() {
		return mailingAddress;
	}
	public void setMailingAddress(Address mailingAddress) {
		this.mailingAddress = mailingAddress;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public boolean equals(Object that) {
		if ( !(that instanceof Person) ) return false;
		Person person = (Person) that;
		return person.getName().equals(name);
	}

	public int hashCode() {
		return name.hashCode();
	}
}
