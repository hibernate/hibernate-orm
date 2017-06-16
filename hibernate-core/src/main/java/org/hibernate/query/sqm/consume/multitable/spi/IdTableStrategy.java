/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi;

import org.hibernate.boot.spi.MetadataImplementor;
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

	// todo (6.0) : Andrea, do we want to this to use the runtime model rather than the boot model here?
	//		IIRC we passed the boot model because that was where schema tooling
	// 		happened, but since we are changing that, maybe we should changes this
	//		as well...
	//
	//		additionally (to ^^) there is the question of what exactly the strategy should do
	//		in terms of table creation/dropping.

	/**
	 * Prepare the strategy for use.  Called one time as the SessionFactory
	 * is being built.
	 *
	 * @param runtimeMetadata Access to the runtime mappings
	 * @param connectionAccess Access to a JDBC Connection if needed
	 */
	default void prepare(
			MetadataImplementor runtimeMetadata,
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
	default void release(MetadataImplementor runtimeMetadata, JdbcConnectionAccess connectionAccess) {
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
