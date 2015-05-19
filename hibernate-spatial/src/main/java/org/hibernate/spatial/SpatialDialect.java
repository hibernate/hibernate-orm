/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial;

import java.io.Serializable;

/**
 * Describes the features of a spatially enabled dialect.
 *
 * @author Karel Maesen
 */
public interface SpatialDialect extends Serializable {

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
	public String getSpatialRelateSQL(String columnName, int spatialRelation);

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
	public String getSpatialFilterExpression(String columnName);

	/**
	 * Returns the SQL fragment for the specfied Spatial aggregate expression.
	 *
	 * @param columnName The name of the Geometry property
	 * @param aggregation The type of <code>SpatialAggregate</code>
	 *
	 * @return The SQL fragment for the projection
	 */
	public String getSpatialAggregateSQL(String columnName, int aggregation);

	/**
	 * Returns The SQL fragment when parsing a <code>DWithinExpression</code>.
	 *
	 * @param columnName The geometry column to test against
	 *
	 * @return The SQL fragment when parsing a <code>DWithinExpression</code>.
	 */
	public String getDWithinSQL(String columnName);

	/**
	 * Returns the SQL fragment when parsing an <code>HavingSridExpression</code>.
	 *
	 * @param columnName The geometry column to test against
	 *
	 * @return The SQL fragment for an <code>HavingSridExpression</code>.
	 */
	public String getHavingSridSQL(String columnName);


	/**
	 * Returns the SQL fragment when parsing a <code>IsEmptyExpression</code> or
	 * <code>IsNotEmpty</code> expression.
	 *
	 * @param columnName The geometry column
	 * @param isEmpty Whether the geometry is tested for empty or non-empty
	 *
	 * @return The SQL fragment for the isempty function
	 */
	public String getIsEmptySQL(String columnName, boolean isEmpty);

	/**
	 * Returns true if this <code>SpatialDialect</code> supports a specific filtering function.
	 * <p> This is intended to signal DB-support for fast window queries, or MBR-overlap queries.</p>
	 *
	 * @return True if filtering is supported
	 */
	public boolean supportsFiltering();

	/**
	 * Does this dialect supports the specified <code>SpatialFunction</code>.
	 *
	 * @param function <code>SpatialFunction</code>
	 *
	 * @return True if this <code>SpatialDialect</code> supports the spatial function specified by the function parameter.
	 */
	public boolean supports(SpatialFunction function);

}
