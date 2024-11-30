/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named;

import org.hibernate.Incubating;

/**
 * Contract for Query impls that can be converted to a named query memento to be
 * stored in the {@link NamedObjectRepository}
 *
 * @author Steve Ebersole
 */
@Incubating
public interface NameableQuery {
	/**
	 * Convert the query into the memento
	 */
	NamedQueryMemento<?> toMemento(String name);
}
