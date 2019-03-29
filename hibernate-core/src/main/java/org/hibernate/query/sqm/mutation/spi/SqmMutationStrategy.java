/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi;

import org.hibernate.Metamodel;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

/**
 * Pluggable strategy for defining how mutation (`UPDATE` or `DELETE`)
 * queries should be handled.
 *
 * {@link #prepare} and {@link #release} allow the strategy to perform
 * any one time preparation and cleanup.
 *
 * The heavy lifting is handled by the {@link UpdateHandler} and {@link DeleteHandler}
 * delegates obtained via {@link #buildUpdateHandler} and {@link #buildDeleteHandler}
 * methods.
 *
 * @apiNote See {@link SqmMutationStrategyHelper#resolveStrategy} for standard resolution
 * of the strategy to use.  See also {@link SqmMutationStrategyHelper#resolveDeleteHandler}
 * and {@link SqmMutationStrategyHelper#resolveUpdateHandler} for standard resolution of
 * the delete and update handler to use applying standard special-case handling
 *
 * @author Steve Ebersole
 */
public interface SqmMutationStrategy {

	/**
	 * Prepare the strategy for use.  Called one time as the SessionFactory
	 * is being built.
	 *
	 * @param runtimeMetadata Access to the runtime mappings
	 * @param connectionAccess Access to a JDBC Connection if needed
	 */
	default void prepare(
			Metamodel runtimeMetadata,
			SessionFactoryOptions sessionFactoryOptions,
			JdbcConnectionAccess connectionAccess) {
		// by default, nothing to do...
	}

	/**
	 * Release the strategy.   Called one time as the SessionFactory is
	 * being shut down.
	 *
	 * @param runtimeMetadata Access to the runtime mappings
	 * @param connectionAccess Access to the JDBC Connection
	 */
	default void release(Metamodel runtimeMetadata, JdbcConnectionAccess connectionAccess) {
		// by default, nothing to do...
	}

	/**
	 * Build a handler capable of handling the update query indicated by the given SQM tree.
	 *
	 * @param sqmUpdateStatement The SQM AST representing the update query
	 * @param domainParameterXref cross references between SqmParameters and QueryParameters
	 * @param creationContext Context info for the creation
	 */
	UpdateHandler buildUpdateHandler(
			SqmUpdateStatement sqmUpdateStatement,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext);

	/**
	 * Build a handler capable of handling the delete query indicated by the given SQM tree.
	 *
	 * @param sqmDeleteStatement The SQM AST representing the delete query
	 * @param domainParameterXref cross references between SqmParameters
	 * @param creationContext Context info for the creation
	 */
	DeleteHandler buildDeleteHandler(
			SqmDeleteStatement sqmDeleteStatement,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext);
}
