/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model;

import java.util.List;

import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.jdbc.Expectation;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;

/**
 * MutationOperation that is capable of being handled as a
 * JDBC {@link java.sql.PreparedStatement}
 *
 * Person ( PERSON, PERSON_SUPP )
 *
 * 		- PERSON_SUPP is optional secondary table
 *
 * @author Steve Ebersole
 */
public interface PreparableMutationOperation extends MutationOperation {
	/**
	 * The SQL to be used when creating the PreparedStatement
	 */
	String getSqlString();

	/**
	 * Get the list of parameter binders for the generated PreparedStatement
	 */
	List<JdbcParameterBinder> getParameterBinders();

	/**
	 * Whether the operation is callable
	 */
	boolean isCallable();

	/**
	 * The expected outcome of execution
	 */
	Expectation getExpectation();

	/**
	 * Series of opt-out checks for whether the operation can be
	 * handled as part of a batch.
	 *
	 * @implNote This does not account for whether batching is enabled
	 * or not on the factory, just whether we can potentially batch it
	 * relative to the operation itself
	 */
	default boolean canBeBatched(BatchKey batchKey, int batchSize) {
		if ( batchKey == null || batchSize < 2 ) {
			return false;
		}

		// This should already be guaranteed by the batchKey being null
		assert !getTableDetails().isIdentifierTable() ||
				!( getMutationTarget() instanceof EntityMutationTarget
						&& ( (EntityMutationTarget) getMutationTarget() ).getMutationDelegate( getMutationType() ) != null );

		if ( getMutationType() == MutationType.UPDATE ) {
			// we cannot batch updates against optional tables
			if ( getTableDetails().isOptional() ) {
				return false;
			}
		}

		return getExpectation().canBeBatched();
	}
}
