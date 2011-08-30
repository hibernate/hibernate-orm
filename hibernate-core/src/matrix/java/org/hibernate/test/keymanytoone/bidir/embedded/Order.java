package org.hibernate.test.keymanytoone.bidir.embedded;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class Order implements Serializable {
	private Customer customer;
	private long number;
	private Set items = new HashSet();

	public Order() {
	}

	public Order(Customer customer, long number) {
		this.customer = customer;
		this.number = number;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public long getNumber() {
		return number;
	}

	public void setNumber(long number) {
		this.number = number;
	}

	public Set getItems() {
		return items;
	}

	public void setItems(Set items) {
		this.items = items;
	}
}
