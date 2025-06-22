/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.Filter;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * Things that can have associated {@link org.hibernate.annotations.Filter} declarations.
 *
 * @see WhereRestrictable
 */
public interface FilterRestrictable {

	/**
	 * Applies just the {@link org.hibernate.annotations.Filter}
	 * values enabled for the associated entity
	 * @deprecated Use {@link #applyFilterRestrictions(Consumer, TableGroup, boolean, Map, boolean, SqlAstCreationState)} instead
	 */
	@Deprecated(forRemoval = true)
	default void applyFilterRestrictions(
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			boolean useQualifier,
			Map<String, Filter> enabledFilters,
			SqlAstCreationState creationState) {
		applyFilterRestrictions( predicateConsumer, tableGroup, useQualifier, enabledFilters, false, creationState );
	}

	/**
	 * Applies just the {@link org.hibernate.annotations.Filter}
	 * values enabled for the associated entity
	 */
	void applyFilterRestrictions(
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			boolean useQualifier,
			Map<String, Filter> enabledFilters,
			boolean onlyApplyLoadByKeyFilters,
			SqlAstCreationState creationState);
}
