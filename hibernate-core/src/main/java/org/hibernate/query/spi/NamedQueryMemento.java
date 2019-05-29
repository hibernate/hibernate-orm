/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Base contract for all named query mementos
 *
 * @author Steve Ebersole
 */
public interface NamedQueryMemento {
	/**
	 * The name under which the query is registered
	 */
	String getName();

	/**
	 * Makes a copy of the memento
	 */
	NamedQueryMemento makeCopy(String name);

	/**
	 * Convert the memento into an executable query
	 */
	<T> QueryImplementor<T> toQuery(SharedSessionContractImplementor session, Class<T> resultType);
}
