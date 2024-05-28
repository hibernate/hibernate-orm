/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.jdbc.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;

/**
 * The "resolved" form of {@link JdbcValuesMapping} providing access
 * to resolved ({@link DomainResultAssembler}) descriptors and resolved
 * initializer ({@link Initializer}) descriptors.
 *
 * @see JdbcValuesMapping#resolveAssemblers(SessionFactoryImplementor)
 */
public interface JdbcValuesMappingResolution {

	DomainResultAssembler<?>[] getDomainResultAssemblers();

	Initializer<?>[] getResultInitializers();

	Initializer<?>[] getInitializers();

	Initializer<?>[] getSortedForResolveInstance();

	boolean hasCollectionInitializers();
}
