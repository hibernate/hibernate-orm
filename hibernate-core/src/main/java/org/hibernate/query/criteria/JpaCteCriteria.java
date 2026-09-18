/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.Arrays;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.Incubating;
import org.hibernate.sql.ast.spi.query.cte.CteMaterialization;
import org.hibernate.sql.ast.spi.query.cte.CteSearchClauseKind;

/**
 * A CTE (common table expression) criteria.
 */
@Incubating(since = "6.3")
public interface JpaCteCriteria<T> extends JpaCriteriaNode {

	/**
	 * The name under which this CTE is registered.
	 */
	@Nullable
	String getName();

	/**
	 * The type of the CTE.
	 */
	@Nonnull
	JpaCteCriteriaType<T> getType();

	/**
	 * The definition of the CTE.
	 */
	@Nonnull
	JpaSelectCriteria<?> getCteDefinition();

	/**
	 * The container within this CTE is registered.
	 */
	@Nonnull
	JpaCteContainer getCteContainer();

	/**
	 * The materialization hint for the CTE.
	 */
	@Nonnull
	CteMaterialization getMaterialization();
	/**
	 * Set the CTE materialization hint.
	 */
	void setMaterialization(@Nonnull CteMaterialization materialization);

	/**
	 * The kind of search (breadth-first or depth-first) that should be done for a recursive query.
	 * May be null if unspecified or if this is not a recursive query.
	 */
	@Nullable
	CteSearchClauseKind getSearchClauseKind();

	/**
	 * The order by which should be searched.
	 */
	@Nonnull
	List<JpaSearchOrder> getSearchBySpecifications();

	/**
	 * The attribute name by which one can order the final CTE result, to achieve the search order.
	 * Note that an implicit {@link JpaCteCriteriaAttribute} will be made available for this.
	 */
	@Nullable
	String getSearchAttributeName();

	/**
	 * Define the CTE search clause.
	 */
	default void search(@Nullable CteSearchClauseKind kind, @Nullable String searchAttributeName, @Nonnull JpaSearchOrder... searchOrders) {
		search( kind, searchAttributeName, Arrays.asList( searchOrders ) );
	}

	/**
	 * Define the CTE search clause.
	 */
	void search(@Nullable CteSearchClauseKind kind, @Nullable String searchAttributeName, @Nullable List<JpaSearchOrder> searchOrders);

	/**
	 * The attributes to use for cycle detection.
	 */
	@Nonnull
	List<JpaCteCriteriaAttribute> getCycleAttributes();

	/**
	 * The attribute name which is used to mark when a cycle has been detected.
	 * Note that an implicit {@link JpaCteCriteriaAttribute} will be made available for this.
	 */
	@Nullable
	String getCycleMarkAttributeName();

	/**
	 * The attribute name that represents the computation path, which is used for cycle detection.
	 * Note that an implicit {@link JpaCteCriteriaAttribute} will be made available for this.
	 */
	@Nullable
	String getCyclePathAttributeName();

	/**
	 * The value which is set for the cycle mark attribute when a cycle is detected.
	 */
	@Nullable
	Object getCycleValue();

	/**
	 * The default value for the cycle mark attribute when no cycle is detected.
	 */
	@Nullable
	Object getNoCycleValue();

	/**
	 * Define the CTE cycle clause.
	 */
	default void cycle(@Nullable String cycleMarkAttributeName, @Nonnull JpaCteCriteriaAttribute... cycleColumns) {
		cycleUsing( cycleMarkAttributeName, null, Arrays.asList( cycleColumns ) );
	}

	/**
	 * Define the CTE cycle clause.
	 */
	default void cycle(@Nullable String cycleMarkAttributeName, @Nullable List<JpaCteCriteriaAttribute> cycleColumns) {
		cycleUsing( cycleMarkAttributeName, null, true, false, cycleColumns );
	}

	/**
	 * Define the CTE cycle clause with an explicit path attribute.
	 */
	default void cycleUsing(@Nullable String cycleMarkAttributeName, @Nullable String cyclePathAttributeName, @Nonnull JpaCteCriteriaAttribute... cycleColumns) {
		cycleUsing( cycleMarkAttributeName, cyclePathAttributeName, Arrays.asList( cycleColumns ) );
	}

	/**
	 * Define the CTE cycle clause with an explicit path attribute.
	 */
	default void cycleUsing(@Nullable String cycleMarkAttributeName, @Nullable String cyclePathAttributeName, @Nullable List<JpaCteCriteriaAttribute> cycleColumns) {
		cycleUsing( cycleMarkAttributeName, cyclePathAttributeName, true, false, cycleColumns );
	}

	/**
	 * Define the CTE cycle clause.
	 */
	default <X> void cycle(@Nullable String cycleMarkAttributeName, @Nullable X cycleValue, @Nullable X noCycleValue, @Nonnull JpaCteCriteriaAttribute... cycleColumns) {
		cycleUsing( cycleMarkAttributeName, null, cycleValue, noCycleValue, Arrays.asList( cycleColumns ) );
	}

	/**
	 * Define the CTE cycle clause.
	 */
	default <X> void cycle(@Nullable String cycleMarkAttributeName, @Nullable X cycleValue, @Nullable X noCycleValue, @Nullable List<JpaCteCriteriaAttribute> cycleColumns) {
		cycleUsing( cycleMarkAttributeName, null, cycleValue, noCycleValue, cycleColumns );
	}

	/**
	 * Define the CTE cycle clause with an explicit path attribute.
	 */
	default <X> void cycleUsing(@Nullable String cycleMarkAttributeName, @Nullable String cyclePathAttributeName, @Nullable X cycleValue, @Nullable X noCycleValue, @Nonnull JpaCteCriteriaAttribute... cycleColumns) {
		cycleUsing( cycleMarkAttributeName, cyclePathAttributeName, cycleValue, noCycleValue, Arrays.asList( cycleColumns ) );
	}

	/**
	 * Define the CTE cycle clause with an explicit path attribute.
	 */
	<X> void cycleUsing(@Nullable String cycleMarkAttributeName, @Nullable String cyclePathAttributeName, @Nullable X cycleValue, @Nullable X noCycleValue, @Nullable List<JpaCteCriteriaAttribute> cycleColumns);
}
