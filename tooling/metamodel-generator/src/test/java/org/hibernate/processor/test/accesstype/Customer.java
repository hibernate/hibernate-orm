/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.accesstype;

import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;

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
