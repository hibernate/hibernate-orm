/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.mapjoin;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity
public class Customer {

	@Id
	@GeneratedValue
	private int id;
	private String name;
	@OneToMany(cascade = CascadeType.ALL)
	private Map<String, CustomerOrder> orderMap;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, CustomerOrder> getOrderMap() {
		return orderMap;
	}

	public void setOrderMap(Map<String, CustomerOrder> orderMap) {
		this.orderMap = orderMap;
	}

	public void addOrder(String orderType, String itemName, int qty) {
		if ( orderMap == null ) {
			orderMap = new HashMap<>();
		}
		CustomerOrder order = new CustomerOrder();
		order.setItem( itemName );
		order.setQty( qty );
		orderMap.put( orderType, order );
	}

	@Override
	public String toString() {
		return "Customer{" +
				"id=" + id +
				", name='" + name + '\'' +
				", orderMap=" + orderMap +
				'}';
	}
}
