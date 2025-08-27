/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.metamodel.mapping.ordering.OrderByFragment;
import org.hibernate.persister.entity.EntityNameUse;
import org.hibernate.query.sqm.spi.SqmCreationContext;
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

	default SqmCreationContext getSqmCreationContext() {
		return getCreationContext().getSessionFactory().getQueryEngine().getCriteriaBuilder();
	}

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
