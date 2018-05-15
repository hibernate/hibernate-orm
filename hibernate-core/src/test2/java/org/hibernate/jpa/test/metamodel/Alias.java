/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel;
import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
@Table(name = "ALIAS_TABLE")
public class Alias implements java.io.Serializable {
	private String id;
	private String alias;
	private Customer customerNoop;
	private Collection<Customer> customersNoop = new java.util.ArrayList<Customer>();
	private Collection<Customer> customers = new java.util.ArrayList<Customer>();

	public Alias() {
	}

	public Alias(String id, String alias) {
		this.id = id;
		this.alias = alias;
	}

	@Id
	@Column(name = "ID")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Column(name = "ALIAS")
	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "FK1_FOR_CUSTOMER_TABLE", insertable = false, updatable = false)
	public Customer getCustomerNoop() {
		return customerNoop;
	}

	public void setCustomerNoop(Customer customerNoop) {
		this.customerNoop = customerNoop;
	}

	@ManyToMany(cascade = CascadeType.ALL)
	@JoinTable(name = "FKS_ANOOP_CNOOP",
			joinColumns =
			@JoinColumn(
					name = "FK2_FOR_ALIAS_TABLE", referencedColumnName = "ID"),
			inverseJoinColumns =
			@JoinColumn(
					name = "FK8_FOR_CUSTOMER_TABLE", referencedColumnName = "ID")
	)
	public Collection<Customer> getCustomersNoop() {
		return customersNoop;
	}

	public void setCustomersNoop(Collection<Customer> customersNoop) {
		this.customersNoop = customersNoop;

	}

	@ManyToMany(cascade = CascadeType.ALL)
	@JoinTable(name = "FKS_ALIAS_CUSTOMER",
			joinColumns =
			@JoinColumn(
					name = "FK_FOR_ALIAS_TABLE", referencedColumnName = "ID"),
			inverseJoinColumns =
			@JoinColumn(
					name = "FK_FOR_CUSTOMER_TABLE", referencedColumnName = "ID")
	)
	public Collection<Customer> getCustomers() {
		return customers;
	}

	public void setCustomers(Collection<Customer> customers) {
		this.customers = customers;
	}

}
