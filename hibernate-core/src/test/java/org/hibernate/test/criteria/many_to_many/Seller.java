package org.hibernate.test.criteria.many_to_many;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

/**
 * @author Janario Oliveira
 */
@Entity
public class Seller {
	@Id
	@GeneratedValue
	private Integer id;
	private String name;

	@ManyToMany
	@JoinTable(name = "seller_customer",
			joinColumns = @JoinColumn(name = "seller_id"),
			inverseJoinColumns = @JoinColumn(name = "customer_id"))
	private Set<Customer> soldTo = new HashSet<Customer>();

	protected Seller() {
	}

	public Seller(String name) {
		this.name = name;
	}

	public Set<Customer> getSoldTo() {
		return soldTo;
	}

	public void addCustomer(Customer customer) {
		customer.getBoughtFrom().add( this );
		soldTo.add( customer );
	}

	@Override
	public boolean equals(Object o) {
		if ( !( o instanceof Seller ) ) {
			return false;
		}

		Seller seller = (Seller) o;
		return name.equals( seller.name );
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}