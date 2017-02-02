/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.postgis;

import java.util.Map;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;

/**
 * Extends the {@code PostgreSQL95Dialect} to add support for the Postgis spatial types, functions and operators .
 * Created by Karel Maesen, Geovise BVBA on 01/11/16.
 */
public class PostgisPG95Dialect extends PostgreSQL95Dialect implements SpatialDialect {


	transient private PostgisSupport support = new PostgisSupport();

	/**
	 * Creates an instance
	 */
	public PostgisPG95Dialect() {
		super();
		registerColumnType(
				PGGeometryTypeDescriptor.INSTANCE.getSqlType(),
				"GEOMETRY"
		);
		for ( Map.Entry<String, SQLFunction> entry : support.functionsToRegister() ) {
			registerFunction( entry.getKey(), entry.getValue() );
		}
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(
				typeContributions,
				serviceRegistry
		);
		support.contributeTypes( typeContributions, serviceRegistry );
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
	public String getSpatialRelateSQL(String columnName, int spatialRelation) {
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
	public String getSpatialFilterExpression(String columnName) {
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
	public String getSpatialAggregateSQL(String columnName, int aggregation) {
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
	public String getDWithinSQL(String columnName) {
		return support.getDWithinSQL( columnName );
	}

	/**
	 * Returns the SQL fragment when parsing an <code>HavingSridExpression</code>.
	 *
	 * @param columnName The geometry column to test against
	 *
	 * @return The SQL fragment for an <code>HavingSridExpression</code>.
	 */
	@Override
	public String getHavingSridSQL(String columnName) {
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
	public String getIsEmptySQL(String columnName, boolean isEmpty) {
		return support.getIsEmptySQL( columnName, isEmpty );
	}

	/**
	 * Returns true if this <code>SpatialDialect</code> supports a specific filtering function.
	 * <p> This is intended to signal DB-support for fast window queries, or MBR-overlap queries.</p>
	 *
	 * @return True if filtering is supported
	 */
	@Override
	public boolean supportsFiltering() {
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
	public boolean supports(SpatialFunction function) {
		return support.supports( function );
	}
}
