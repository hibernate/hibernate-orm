/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;

/**
 * A context for execution of SQL statements expressed via
 * SQL AST and JdbcOperation
 */
public interface ExecutionContext {

	default boolean isScrollResult(){
		return false;
	}

	SharedSessionContractImplementor getSession();

	QueryOptions getQueryOptions();

	LoadQueryInfluencers getLoadQueryInfluencers();

	QueryParameterBindings getQueryParameterBindings();

	Callback getCallback();

	default boolean hasCallbackActions() {
		final Callback callback = getCallback();
		return callback != null && callback.hasAfterLoadActions();
	}

	String getQueryIdentifier(String sql);

	/**
	 * Get the collection key for the collection which is to be loaded immediately.
	 */
	default CollectionKey getCollectionKey() {
		return null;
	}

	/**
	 * Should only be used when initializing a bytecode-proxy
	 */
	default Object getEntityInstance() {
		return null;
	}

	default Object getEntityId() {
		return null;
	}

	default String getEntityUniqueKeyAttributePath() {
		return null;
	}

	default Object getEntityUniqueKey() {
		return null;
	}

	default EntityMappingType getRootEntityDescriptor() {
		return null;
	}

	default void registerLoadingEntityHolder(EntityHolder holder) {
		// by default do nothing
	}

	/**
	 * Hook to allow delaying calls to {@link LogicalConnectionImplementor#afterStatement()}.
	 * Mainly used in the case of batching and multi-table mutations
	 *
	 */
	// todo (6.0) : come back and make sure we are calling this at appropriate times.
	// Despite the name, it should be called after a logical group of statements - e.g.,
	// after all of the delete statements against all of the tables for a particular entity
	default void afterStatement(LogicalConnectionImplementor logicalConnection) {
		logicalConnection.afterStatement();
	}

	/**
	 * Determine if the query execution has to be considered by the {@link org.hibernate.stat.Statistics}.
	 *
	 * @return true if the query execution has to be added to the {@link org.hibernate.stat.Statistics}, false otherwise.
	 */
	default boolean hasQueryExecutionToBeAddedToStatistics() {
		return false;
	}

	/**
	 * Does this query return objects that might be already cached
	 * by the session, whose lock mode may need upgrading
	 */
	default boolean upgradeLocks(){
		return false;
	}

}
