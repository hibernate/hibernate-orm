/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.Arrays;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;
import org.hibernate.sql.ast.tree.cte.CteMaterialization;
import org.hibernate.sql.ast.tree.cte.CteSearchClauseKind;

/**
 * A CTE (common table expression) criteria.
 */
@Incubating
public interface JpaCteCriteria<T> extends JpaCriteriaNode {

	/**
	 * The name under which this CTE is registered.
	 */
	@Nullable String getName();

	/**
	 * The type of the CTE.
	 */
	JpaCteCriteriaType<T> getType();

	/**
	 * The definition of the CTE.
	 */
	JpaSelectCriteria<?> getCteDefinition();

	/**
	 * The container within this CTE is registered.
	 */
	JpaCteContainer getCteContainer();

	/**
	 * The materialization hint for the CTE.
	 */
	CteMaterialization getMaterialization();
	void setMaterialization(CteMaterialization materialization);

	/**
	 * The kind of search (breadth-first or depth-first) that should be done for a recursive query.
	 * May be null if unspecified or if this is not a recursive query.
	 */
	@Nullable CteSearchClauseKind getSearchClauseKind();
	/**
	 * The order by which should be searched.
	 */
	List<JpaSearchOrder> getSearchBySpecifications();
	/**
	 * The attribute name by which one can order the final CTE result, to achieve the search order.
	 * Note that an implicit {@link JpaCteCriteriaAttribute} will be made available for this.
	 */
	@Nullable String getSearchAttributeName();

	default void search(CteSearchClauseKind kind, String searchAttributeName, JpaSearchOrder... searchOrders) {
		search( kind, searchAttributeName, Arrays.asList( searchOrders ) );
	}

	void search(CteSearchClauseKind kind, String searchAttributeName, List<JpaSearchOrder> searchOrders);

	/**
	 * The attributes to use for cycle detection.
	 */
	List<JpaCteCriteriaAttribute> getCycleAttributes();

	/**
	 * The attribute name which is used to mark when a cycle has been detected.
	 * Note that an implicit {@link JpaCteCriteriaAttribute} will be made available for this.
	 */
	@Nullable String getCycleMarkAttributeName();

	/**
	 * The attribute name that represents the computation path, which is used for cycle detection.
	 * Note that an implicit {@link JpaCteCriteriaAttribute} will be made available for this.
	 */
	@Nullable String getCyclePathAttributeName();

	/**
	 * The value which is set for the cycle mark attribute when a cycle is detected.
	 */
	@Nullable Object getCycleValue();

	/**
	 * The default value for the cycle mark attribute when no cycle is detected.
	 */
	@Nullable Object getNoCycleValue();

	default void cycle(String cycleMarkAttributeName, JpaCteCriteriaAttribute... cycleColumns) {
		cycleUsing( cycleMarkAttributeName, null, Arrays.asList( cycleColumns ) );
	}

	default void cycle(String cycleMarkAttributeName, List<JpaCteCriteriaAttribute> cycleColumns) {
		cycleUsing( cycleMarkAttributeName, null, true, false, cycleColumns );
	}

	default void cycleUsing(String cycleMarkAttributeName, String cyclePathAttributeName, JpaCteCriteriaAttribute... cycleColumns) {
		cycleUsing( cycleMarkAttributeName, cyclePathAttributeName, Arrays.asList( cycleColumns ) );
	}

	default void cycleUsing(String cycleMarkAttributeName, String cyclePathAttributeName, List<JpaCteCriteriaAttribute> cycleColumns) {
		cycleUsing( cycleMarkAttributeName, cyclePathAttributeName, true, false, cycleColumns );
	}

	default <X> void cycle(String cycleMarkAttributeName, X cycleValue, X noCycleValue, JpaCteCriteriaAttribute... cycleColumns) {
		cycleUsing( cycleMarkAttributeName, null, cycleValue, noCycleValue, Arrays.asList( cycleColumns ) );
	}

	default <X> void cycle(String cycleMarkAttributeName, X cycleValue, X noCycleValue, List<JpaCteCriteriaAttribute> cycleColumns) {
		cycleUsing( cycleMarkAttributeName, null, cycleValue, noCycleValue, cycleColumns );
	}

	default <X> void cycleUsing(String cycleMarkAttributeName, String cyclePathAttributeName, X cycleValue, X noCycleValue, JpaCteCriteriaAttribute... cycleColumns) {
		cycleUsing( cycleMarkAttributeName, cyclePathAttributeName, cycleValue, noCycleValue, Arrays.asList( cycleColumns ) );
	}

	<X> void cycleUsing(String cycleMarkAttributeName, String cyclePathAttributeName, X cycleValue, X noCycleValue, List<JpaCteCriteriaAttribute> cycleColumns);
}
