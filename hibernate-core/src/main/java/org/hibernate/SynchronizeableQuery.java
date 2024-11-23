/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.util.Collection;

/**
 * A unifying interface for queries which can define tables (query spaces) to synchronize on.
 *
 * These query spaces affect the process of auto-flushing by determining which entities will be
 * processed by auto-flush based on the table to which those entities are mapped and which are
 * determined to have pending state changes.
 *
 * In a similar manner, these query spaces also affect how query result caching can recognize
 * invalidated results.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings( { "unused", "UnusedReturnValue", "RedundantSuppression" } )
public interface SynchronizeableQuery<T> {
	/**
	 * Obtain the list of query spaces the query is synchronized on.
	 *
	 * @return The list of query spaces upon which the query is synchronized.
	 */
	Collection<String> getSynchronizedQuerySpaces();

	/**
	 * Adds a query space.
	 *
	 * @param querySpace The query space to be auto-flushed for this query.
	 *
	 * @return {@code this}, for method chaining
	 */
	SynchronizeableQuery<T> addSynchronizedQuerySpace(String querySpace);

	/**
	 * Adds one-or-more synchronized spaces
	 */
	default SynchronizeableQuery<T> addSynchronizedQuerySpace(String... querySpaces) {
		if ( querySpaces != null ) {
			for ( int i = 0; i < querySpaces.length; i++ ) {
				addSynchronizedQuerySpace( querySpaces[i] );
			}
		}
		return this;
	}

	/**
	 * Adds a table expression as a query space.
	 */
	default SynchronizeableQuery<T> addSynchronizedTable(String tableExpression) {
		return addSynchronizedQuerySpace( tableExpression );
	}

	/**
	 * Adds one-or-more synchronized table expressions
	 */
	default SynchronizeableQuery<T> addSynchronizedTable(String... tableExpressions) {
		return addSynchronizedQuerySpace( tableExpressions );
	}

	/**
	 * Adds an entity name for (a) auto-flush checking and (b) query result cache invalidation checking.  Same as
	 * {@link #addSynchronizedQuerySpace} for all tables associated with the given entity.
	 *
	 * @param entityName The name of the entity upon whose defined query spaces we should additionally synchronize.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @throws MappingException Indicates the given name could not be resolved as an entity
	 */
	SynchronizeableQuery<T> addSynchronizedEntityName(String entityName) throws MappingException;

	/**
	 * Adds one-or-more entities (by name) whose tables should be added as synchronized spaces
	 */
	default SynchronizeableQuery<T> addSynchronizedEntityName(String... entityNames) throws MappingException {
		if ( entityNames != null ) {
			for ( int i = 0; i < entityNames.length; i++ ) {
				addSynchronizedEntityName( entityNames[i] );
			}
		}
		return this;
	}

	/**
	 * Adds an entity for (a) auto-flush checking and (b) query result cache invalidation checking.  Same as
	 * {@link #addSynchronizedQuerySpace} for all tables associated with the given entity.
	 *
	 * @param entityClass The class of the entity upon whose defined query spaces we should additionally synchronize.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @throws MappingException Indicates the given class could not be resolved as an entity
	 */
	@SuppressWarnings( "rawtypes" )
	SynchronizeableQuery<T> addSynchronizedEntityClass(Class entityClass) throws MappingException;

	/**
	 * Adds one-or-more entities (by class) whose tables should be added as synchronized spaces
	 */
	default SynchronizeableQuery<T> addSynchronizedEntityClass(Class<?>... entityClasses) throws MappingException {
		if ( entityClasses != null ) {
			for ( int i = 0; i < entityClasses.length; i++ ) {
				addSynchronizedEntityClass( entityClasses[i] );
			}
		}
		return this;
	}
}
