/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.query;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.NamedQueryMemento;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Common attributes shared across the mapping of named HQL, native
 * and "callable" queries defined in annotations, orm.xml and hbm.xml
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface NamedQueryDefinition<E> {
	/**
	 * The name under which the query is to be registered
	 */
	String getRegistrationName();

	/**
	 * The expected result type of the query, or {@code null}.
	 */
	@Nullable
	Class<E> getResultType();

	/**
	 * Resolve the mapping definition into its run-time memento form
	 */
	NamedQueryMemento<E> resolve(SessionFactoryImplementor factory);
}
