/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.accesstype;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Access;
import javax.persistence.AccessType;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Customer extends User {
	private Set<Order> orders;
	private String nonPersistent;

	@Access(AccessType.FIELD)
	boolean goodPayer;

	public Set<Order> getOrders() {
		return orders;
	}

	@OneToMany
	public void setOrders(Set<Order> orders) {
		this.orders = orders;
	}
}
