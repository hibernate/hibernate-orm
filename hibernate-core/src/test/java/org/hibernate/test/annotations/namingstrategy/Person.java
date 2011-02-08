// $Id$
package org.hibernate.test.annotations.namingstrategy;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity
public class Person {

	@Id
	private long id;

	@OneToMany(mappedBy = "person")
	private Set<Address> addresses = new HashSet<Address>();

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Set<Address> getAddresses() {
		return addresses;
	}

	public void setAddresses(Set<Address> addresses) {
		this.addresses = addresses;
	}
}
