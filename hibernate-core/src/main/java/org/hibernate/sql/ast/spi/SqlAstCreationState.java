/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.metamodel.mapping.ordering.OrderByFragment;
import org.hibernate.persister.entity.EntityNameUse;
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * Access to stuff used while creating a SQL AST
 *
 * @author Steve Ebersole
 */
public interface SqlAstCreationState {
	SqlAstCreationContext getCreationContext();

	SqlAstProcessingState getCurrentProcessingState();

	SqlExpressionResolver getSqlExpressionResolver();

	FromClauseAccess getFromClauseAccess();

	SqlAliasBaseGenerator getSqlAliasBaseGenerator();

	LoadQueryInfluencers getLoadQueryInfluencers();

	boolean applyOnlyLoadByKeyFilters();

	void registerLockMode(String identificationVariable, LockMode explicitLockMode);

	/**
	 * This callback is for handling of filters and is necessary to allow correct treat optimizations.
	 */
	@Internal
	default void registerEntityNameUsage(
			TableGroup tableGroup,
			EntityNameUse entityNameUse,
			String hibernateEntityName) {
		// No-op
	}

	@Internal
	default boolean supportsEntityNameUsage() {
		return false;
	}

	@Internal
	default void applyOrdering(TableGroup tableGroup, OrderByFragment orderByFragment) {
	}

	default boolean isProcedureOrNativeQuery(){
		return false;
	}
}
