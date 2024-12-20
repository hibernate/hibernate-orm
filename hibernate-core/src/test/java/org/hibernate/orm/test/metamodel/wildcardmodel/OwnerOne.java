/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.metamodel.wildcardmodel;

import java.util.List;
import jakarta.persistence.Entity;

@Entity
public class OwnerOne extends AbstractOwner {

	@SuppressWarnings("unchecked")
	@Override
	public List<EntityOne> getEntities() {
		return (List<EntityOne>) super.getEntities();
	}
}
