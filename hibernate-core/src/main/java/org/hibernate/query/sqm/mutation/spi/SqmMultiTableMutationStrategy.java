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
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

/**
 * Pluggable strategy for defining how mutation ({@code UPDATE} or {@code DELETE}) queries should
 * be handled when the target entity is mapped to multiple tables via secondary tables or certain
 * inheritance strategies.
 * <p>
 * The main contracts here are {@link #executeUpdate} and {@link #executeDelete}.
 * <p>
 * The methods {@link #prepare} and {@link #release} allow the strategy to perform any one time
 * preparation and cleanup.
 *
 * @apiNote See {@link SqmMultiTableMutationStrategyProvider#createMutationStrategy} for standard
 *          resolution of the strategy to use for each hierarchy.
 *
 * @author Steve Ebersole
 */
public interface SqmMultiTableMutationStrategy {

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
	 * Builds a cacheable handler for the passed SqmDeleteOrUpdateStatement.
	 *
	 * @return The number of rows affected
	 */
	MultiTableHandlerBuildResult buildHandler(SqmDeleteOrUpdateStatement<?> sqmStatement, DomainParameterXref domainParameterXref, DomainQueryExecutionContext context);

	/**
	 * Execute the multi-table update indicated by the passed SqmUpdateStatement
	 *
	 * @return The number of rows affected
	 * @deprecated Use {@link #buildHandler(SqmDeleteOrUpdateStatement, DomainParameterXref, DomainQueryExecutionContext)} instead
	 */
	@Deprecated(forRemoval = true, since = "7.1")
	default int executeUpdate(
			SqmUpdateStatement<?> sqmUpdateStatement,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context) {
		final MultiTableHandlerBuildResult buildResult = buildHandler( sqmUpdateStatement, domainParameterXref, context );
		return buildResult.multiTableHandler().execute( buildResult.firstJdbcParameterBindings(), context );
	}

	/**
	 * Execute the multi-table update indicated by the passed SqmUpdateStatement
	 *
	 * @return The number of rows affected
	 * @deprecated Use {@link #buildHandler(SqmDeleteOrUpdateStatement, DomainParameterXref, DomainQueryExecutionContext)} instead
	 */
	@Deprecated(forRemoval = true, since = "7.1")
	default int executeDelete(
			SqmDeleteStatement<?> sqmDeleteStatement,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context) {
		final MultiTableHandlerBuildResult buildResult = buildHandler( sqmDeleteStatement, domainParameterXref, context );
		return buildResult.multiTableHandler().execute( buildResult.firstJdbcParameterBindings(), context );
	}
}
