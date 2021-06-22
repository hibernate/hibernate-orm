/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.postgis;

import java.sql.Types;

import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.dialect.SpatialFunctionsRegistry;

interface PGSpatialDialectTrait extends SpatialDialect {

	PostgisSupport support = new PostgisSupport();


	default SpatialFunctionsRegistry functionsToRegister() {
		return support.functionsToRegister();
	}
	/**
	 * Returns the SQL fragment for the SQL WHERE-clause when parsing
	 * <code>org.hibernatespatial.criterion.SpatialRelateExpression</code>s
	 * into prepared statements.
	 * <p/>
	 *
	 * @param columnName The name of the geometry-typed column to which the relation is
	 * applied
	 * @param spatialRelation The type of spatial relation (as defined in
	 * <code>SpatialRelation</code>).
	 *
	 * @return SQL fragment  {@code SpatialRelateExpression}
	 */
	@Override
	default String getSpatialRelateSQL(String columnName, int spatialRelation) {
		return support.getSpatialRelateSQL( columnName, spatialRelation );
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
	@Override
	default String getSpatialFilterExpression(String columnName) {
		return support.getSpatialFilterExpression( columnName );
	}

	/**
	 * Returns the SQL fragment for the specfied Spatial aggregate expression.
	 *
	 * @param columnName The name of the Geometry property
	 * @param aggregation The type of <code>SpatialAggregate</code>
	 *
	 * @return The SQL fragment for the projection
	 */
	@Override
	default String getSpatialAggregateSQL(String columnName, int aggregation) {
		return support.getSpatialAggregateSQL( columnName, aggregation );
	}

	/**
	 * Returns The SQL fragment when parsing a <code>DWithinExpression</code>.
	 *
	 * @param columnName The geometry column to test against
	 *
	 * @return The SQL fragment when parsing a <code>DWithinExpression</code>.
	 */
	@Override
	default String getDWithinSQL(String columnName) {
		return support.getDWithinSQL( columnName );
	}

	/**
	 * Returns the SQL fragment when parsing a <code>HavingSridExpression</code>.
	 *
	 * @param columnName The geometry column to test against
	 *
	 * @return The SQL fragment for a <code>HavingSridExpression</code>.
	 */
	@Override
	default String getHavingSridSQL(String columnName) {
		return support.getHavingSridSQL( columnName );
	}

	/**
	 * Returns the SQL fragment when parsing a <code>IsEmptyExpression</code> or
	 * <code>IsNotEmpty</code> expression.
	 *
	 * @param columnName The geometry column
	 * @param isEmpty Whether the geometry is tested for empty or non-empty
	 *
	 * @return The SQL fragment for the isempty function
	 */
	@Override
	default String getIsEmptySQL(String columnName, boolean isEmpty) {
		return support.getIsEmptySQL( columnName, isEmpty );
	}

	/**
	 * Returns true if this <code>SpatialDialect</code> supports a specific filtering function.
	 * <p> This is intended to signal DB-support for fast window queries, or MBR-overlap queries.</p>
	 *
	 * @return True if filtering is supported
	 */
	@Override
	default boolean supportsFiltering() {
		return support.supportsFiltering();
	}

	/**
	 * Does this dialect supports the specified <code>SpatialFunction</code>.
	 *
	 * @param function <code>SpatialFunction</code>
	 *
	 * @return True if this <code>SpatialDialect</code> supports the spatial function specified by the function parameter.
	 */
	@Override
	default boolean supports(SpatialFunction function) {
		return support.supports( function );
	}

	/**
	 * Checks whether the typeCode is (potentially) the code for a spatial type
	 * @param typeCode the JDBC type code
	 * @return if the typecode corresponds with a spatialt type
	 */
	default boolean isSpatial(int typeCode){
		return support.isSpatial( typeCode );
	}
}
