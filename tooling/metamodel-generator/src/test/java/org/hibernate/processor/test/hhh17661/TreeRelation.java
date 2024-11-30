/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.hhh17661;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class TreeRelation<T extends Tree<T, ? extends TreeRelation<T>>> extends Entity {

	@ManyToOne(optional = false)
	private T parent;

	@ManyToOne(optional = false)
	private T child;
}
