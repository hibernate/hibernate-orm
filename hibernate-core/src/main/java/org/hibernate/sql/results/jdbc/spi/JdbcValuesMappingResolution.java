/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
