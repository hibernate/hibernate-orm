/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi;

import org.hibernate.Metamodel;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.query.sqm.tree.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;

/**
 * Contract for pluggable strategy for defining how mutation (`UPDATE`/`DELETE`)
 * queries against entities spanning multi tables.
 *
 * {@link #prepare} and {@link #release} allow the strategy to perform
 * any one time preparation and cleanup.  The heavy lifting is handled by
 * the {@link UpdateHandler} and {@link DeleteHandler} delegates obtained
 * via {@link #buildUpdateHandler} and {@link #buildDeleteHandler} methods.
 *
 * @author Steve Ebersole
 */
public interface IdTableStrategy {

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
	 * @param creationContext The SessionFactory
	 *
	 * @return The handler
	 */
	UpdateHandler buildUpdateHandler(SqmUpdateStatement sqmUpdateStatement, HandlerCreationContext creationContext);

	/**
	 * Build a handler capable of handling the delete query indicated by the given SQM tree.
	 *
	 * @param sqmDeleteStatement The SQM AST representing the delete query
	 * @param creationContext The SessionFactory
	 *
	 * @return The handler
	 */
	DeleteHandler buildDeleteHandler(SqmDeleteStatement sqmDeleteStatement, HandlerCreationContext creationContext);
}
