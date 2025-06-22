/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.spi;

import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;

/**
 * Pluggable strategy for defining how insertion (`INSERT`) queries should be handled when the target
 * entity is mapped to multiple tables via secondary tables or certain inheritance strategies or uses an
 * identifier generator that uses an optimizer.
 * <p>
 * {@link #prepare} and {@link #release} allow the strategy to perform any one time preparation and cleanup.
 *
 * @apiNote See {@link SqmMultiTableMutationStrategyProvider#createInsertStrategy} for standard resolution
 *          of the strategy to use for each hierarchy
 *
 * @author Steve Ebersole
 */
public interface SqmMultiTableInsertStrategy {

	/**
	 * Prepare the strategy for use.  Called one time as the SessionFactory
	 * is being built.
	 */
	default void prepare(
			MappingModelCreationProcess mappingModelCreationProcess,
			JdbcConnectionAccess connectionAccess) {
		// by default, nothing to do...
	}

	/**
	 * Prepare the strategy for use.  Called one time as the SessionFactory
	 * is being built.
	 */
	default void prepare(MappingModelCreationProcess mappingModelCreationProcess) {
		prepare( mappingModelCreationProcess,
				mappingModelCreationProcess.getCreationContext().getJdbcServices().getBootstrapJdbcConnectionAccess() );
	}

	/**
	 * Release the strategy.   Called one time as the SessionFactory is
	 * being shut down.
	 */
	default void release(SessionFactoryImplementor sessionFactory, JdbcConnectionAccess connectionAccess) {
		// by default, nothing to do...
	}

	/**
	 * Execute the multi-table insert indicated by the passed SqmInsertStatement
	 *
	 * @return The number of rows affected
	 */
	int executeInsert(
			SqmInsertStatement<?> sqmInsertStatement,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context);

}
