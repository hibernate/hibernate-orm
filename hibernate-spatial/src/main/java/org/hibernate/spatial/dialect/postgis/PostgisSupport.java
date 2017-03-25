/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.postgis;

import java.io.Serializable;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.GeolatteGeometryType;
import org.hibernate.spatial.JTSGeometryType;
import org.hibernate.spatial.SpatialAggregate;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.SpatialRelation;

/**
 * Created by Karel Maesen, Geovise BVBA on 29/10/16.
 */
public class PostgisSupport implements SpatialDialect, Serializable {


	private PostgisFunctions postgisFunctions = new PostgisFunctions();

	void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		typeContributions.contributeType( new GeolatteGeometryType( PGGeometryTypeDescriptor.INSTANCE ) );
		typeContributions.contributeType( new JTSGeometryType( PGGeometryTypeDescriptor.INSTANCE ) );
	}

	public PostgisFunctions functionsToRegister() {
		return postgisFunctions;
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
		switch ( spatialRelation ) {
			case SpatialRelation.WITHIN:
				return " ST_within(" + columnName + ",?)";
			case SpatialRelation.CONTAINS:
				return " ST_contains(" + columnName + ", ?)";
			case SpatialRelation.CROSSES:
				return " ST_crosses(" + columnName + ", ?)";
			case SpatialRelation.OVERLAPS:
				return " ST_overlaps(" + columnName + ", ?)";
			case SpatialRelation.DISJOINT:
				return " ST_disjoint(" + columnName + ", ?)";
			case SpatialRelation.INTERSECTS:
				return " ST_intersects(" + columnName
						+ ", ?)";
			case SpatialRelation.TOUCHES:
				return " ST_touches(" + columnName + ", ?)";
			case SpatialRelation.EQUALS:
				return " ST_equals(" + columnName + ", ?)";
			default:
				throw new IllegalArgumentException(
						"Spatial relation is not known by this dialect"
				);
		}
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
		return "(" + columnName + " && ? ) ";
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
		switch ( aggregation ) {
			case SpatialAggregate.EXTENT:
				final StringBuilder stbuf = new StringBuilder();
				stbuf.append( "st_extent(" ).append( columnName ).append( ")::geometry" );
				return stbuf.toString();
			default:
				throw new IllegalArgumentException(
						"Aggregation of type "
								+ aggregation + " are not supported by this dialect"
				);
		}
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
		return "ST_DWithin(" + columnName + ",?,?)";
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
		return "( ST_srid(" + columnName + ") = ?)";
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
		final String emptyExpr = " ST_IsEmpty(" + columnName + ") ";
		return isEmpty ? emptyExpr : "( NOT " + emptyExpr + ")";
	}

	/**
	 * Returns true if this <code>SpatialDialect</code> supports a specific filtering function.
	 * <p> This is intended to signal DB-support for fast window queries, or MBR-overlap queries.</p>
	 *
	 * @return True if filtering is supported
	 */
	@Override
	public boolean supportsFiltering() {
		return true;
	}

	/**
	 * Does this dialect supports the specified <code>SpatialFunction</code>.
	 *
	 * @param function <code>SpatialFunction</code>
	 *
	 * @return True if this <code>SpatialDialect</code> supports the spatial function specified by the function parameter.
	 */
	@Override
	public boolean supports( SpatialFunction function) {
		return (postgisFunctions.get( function.toString() ) != null);
	}
}
