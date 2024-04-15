/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.cockroachdb;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.dialect.SpatialFunctionsRegistry;

public interface CockroachSpatialDialectTrait extends SpatialDialect {

	CockroachDBSpatialSupport DELEGATE = new CockroachDBSpatialSupport();

	default SpatialFunctionsRegistry functionsToRegister() {
		return DELEGATE.functionsToRegister();

	}

	default String getSpatialRelateSQL(String columnName, int spatialRelation) {
		return DELEGATE.getSpatialRelateSQL( columnName, spatialRelation );
	}

	default void delegateContributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		DELEGATE.contributeTypes( typeContributions, serviceRegistry );
	}

	/**
	 * Returns the SQL fragment for the SQL WHERE-expression when parsing
	 * <code>org.hibernate.spatial.criterion.SpatialFilterExpression</code>s
	 * into prepared statements.
	 *
	 * @param columnName The name of the geometry-typed column to which the filter is
	 * be applied
	 *
	 * @return Rhe SQL fragment for the {@code SpatialFilterExpression}
	 */
	default String getSpatialFilterExpression(String columnName) {
		return DELEGATE.getSpatialFilterExpression( columnName );
	}

	@Override
	default String getSpatialAggregateSQL(String columnName, int aggregation) {
		return DELEGATE.getSpatialAggregateSQL( columnName, aggregation );
	}

	@Override
	default String getDWithinSQL(String columnName) {
		return DELEGATE.getDWithinSQL( columnName );
	}

	@Override
	default String getHavingSridSQL(String columnName) {
		return DELEGATE.getHavingSridSQL( columnName );
	}

	@Override
	default String getIsEmptySQL(String columnName, boolean isEmpty) {
		return DELEGATE.getIsEmptySQL( columnName, isEmpty );
	}

	@Override
	default boolean supportsFiltering() {
		return DELEGATE.supportsFiltering();
	}

	@Override
	default boolean supports(SpatialFunction function) {
		return DELEGATE.supports( function );
	}


	default boolean isSpatial(int typeCode) {
		return DELEGATE.isSpatial( typeCode );
	}

}
