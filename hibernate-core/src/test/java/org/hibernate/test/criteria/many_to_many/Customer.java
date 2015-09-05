package org.hibernate.test.criteria.many_to_many;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

/**
 * @author Janario Oliveira
 */
@Entity
public class Customer {
	@Id
	@GeneratedValue
	private Integer id;
	private String name;

	@ManyToMany(mappedBy = "soldTo")
	private Set<Seller> boughtFrom = new HashSet<Seller>();

	protected Customer() {
	}

	public Customer(String name) {
		this.name = name;
	}

	public Set<Seller> getBoughtFrom() {
		return boughtFrom;
	}

	@Override
	public boolean equals(Object o) {
		if ( !( o instanceof Customer ) ) {
			return false;
		}

		Customer customer = (Customer) o;
		return name.equals( customer.name );

	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}