/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.namingstrategy.complete;

import java.util.List;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Version;

/**
 * @author Steve Ebersole
 */
@Entity( name="CuStOmEr" )
public class Customer {
	private Integer id;
	private Integer version;
	private String name;
	private Set<String> registeredTrademarks;

	private Address hqAddress;
	private Set<Address> addresses;

	private List<Order> orders;

	private Set<Industry> industries;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Version
	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	@Basic
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ElementCollection
	public Set<String> getRegisteredTrademarks() {
		return registeredTrademarks;
	}

	public void setRegisteredTrademarks(Set<String> registeredTrademarks) {
		this.registeredTrademarks = registeredTrademarks;
	}

	@Embedded
	public Address getHqAddress() {
		return hqAddress;
	}

	public void setHqAddress(Address hqAddress) {
		this.hqAddress = hqAddress;
	}

	@ElementCollection
	@Embedded
	public Set<Address> getAddresses() {
		return addresses;
	}

	public void setAddresses(Set<Address> addresses) {
		this.addresses = addresses;
	}

	@OneToMany( mappedBy = "customer" )
	@OrderColumn
	public List<Order> getOrders() {
		return orders;
	}

	public void setOrders(List<Order> orders) {
		this.orders = orders;
	}

	@ManyToMany
	public Set<Industry> getIndustries() {
		return industries;
	}

	public void setIndustries(Set<Industry> industries) {
		this.industries = industries;
	}
}
