/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named;

import jakarta.persistence.Query;
import org.hibernate.Incubating;

/**
 * Contract for {@linkplain org.hibernate.query.spi.QueryImplementor query implementations}
 * which can be converted to {@linkplain  NamedQueryMemento named query mementos} for storage
 * in the {@link NamedObjectRepository}.
 *
 * @author Steve Ebersole
 *
 * @see NamedQueryMemento
 */
@Incubating
public interface NameableQuery {
	/**
	 * Convert this query into the memento.
	 *
	 * @see org.hibernate.SessionFactory#addNamedQuery(String, Query)
	 * @see NamedObjectRepository#registerNamedQuery(String, Query)
	 */
	NamedQueryMemento<?> toMemento(String name);
}
