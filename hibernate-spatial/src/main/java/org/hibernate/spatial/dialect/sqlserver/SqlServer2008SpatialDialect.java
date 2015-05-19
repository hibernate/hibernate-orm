/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.sqlserver;


import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.SQLServer2008Dialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;

import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.GeolatteGeometryType;
import org.hibernate.spatial.JTSGeometryType;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.SpatialRelation;
import org.hibernate.type.StandardBasicTypes;

/**
 * The <code>SpatialDialect</code> for Microsoft SQL Server (2008).
 *
 * @author Karel Maesen, Martin Steinwender.
 */
public class SqlServer2008SpatialDialect extends SQLServer2008Dialect implements SpatialDialect {

	/**
	 * The short name for this dialect
	 */
	public static final String SHORT_NAME = "sqlserver";

	/**
	 * Constructs an instance
	 */
	public SqlServer2008SpatialDialect() {
		super();

		registerColumnType(
				SqlServer2008GeometryTypeDescriptor.INSTANCE.getSqlType(),
				"GEOMETRY"
		);

		// registering OGC functions
		// (spec_simplefeatures_sql_99-04.pdf)

		// section 2.1.1.1
		// Registerfunction calls for registering geometry functions:
		// first argument is the OGC standard functionname,
		// second the Function as it occurs in the spatial dialect
		registerFunction( "dimension", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "?1.STDimension()" ) );
		registerFunction( "geometrytype", new SQLFunctionTemplate( StandardBasicTypes.STRING, "?1.STGeometryType()" ) );
		registerFunction( "srid", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "?1.STSrid" ) );
		registerFunction( "envelope", new SqlServerMethod( "STEnvelope" ) );
		registerFunction( "astext", new SQLFunctionTemplate( StandardBasicTypes.STRING, "?1.STAsText()" ) );
		registerFunction( "asbinary", new SQLFunctionTemplate( StandardBasicTypes.BINARY, "?1.STAsBinary()" ) );

		registerFunction( "isempty", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.STIsEmpty()" ) );
		registerFunction( "issimple", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.STIsSimple()" ) );
		registerFunction( "boundary", new SqlServerMethod( "STBoundary" ) );

		// section 2.1.1.2
		// Register functions for spatial relation constructs
		registerFunction( "contains", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.STContains(?2)" ) );
		registerFunction( "crosses", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.STCrosses(?2)" ) );
		registerFunction( "disjoint", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.STDisjoint(?2)" ) );
		registerFunction( "equals", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.STEquals(?2)" ) );
		registerFunction( "intersects", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.STIntersects(?2)" ) );
		registerFunction( "overlaps", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.STOverlaps(?2)" ) );
		registerFunction( "touches", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.STTouches(?2)" ) );
		registerFunction( "within", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.STWithin(?2)" ) );
		registerFunction( "relate", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.STRelate(?2,?3)" ) );

		// section 2.1.1.3
		// Register spatial analysis functions.
		registerFunction( "distance", new SQLFunctionTemplate( StandardBasicTypes.DOUBLE, "?1.STDistance(?2)" ) );
		registerFunction( "buffer", new SqlServerMethod( "STBuffer" ) );
		registerFunction( "convexhull", new SqlServerMethod( "STConvexHull" ) );
		registerFunction( "difference", new SqlServerMethod( "STDifference" ) );
		registerFunction( "intersection", new SqlServerMethod( "STIntersection" ) );
		registerFunction( "symdifference", new SqlServerMethod( "STSymDifference" ) );
		registerFunction( "geomunion", new SqlServerMethod( "STUnion" ) );
		// we rename OGC union to geomunion because union is a reserved SQL keyword.
		// (See also postgis documentation).

		// portable spatial aggregate functions
		// no aggregatefunctions implemented in sql-server2000
		//registerFunction("extent", new SQLFunctionTemplate(geomType, "?1.STExtent()"));

		// section 2.1.9.1 methods on surfaces
		registerFunction( "area", new SQLFunctionTemplate( StandardBasicTypes.DOUBLE, "?1.STArea()" ) );
		registerFunction( "centroid", new SqlServerMethod( "STCentroid" ) );
		registerFunction(
				"pointonsurface", new SqlServerMethod( "STPointOnSurface" )
		);
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(
				typeContributions,
				serviceRegistry
		);
		typeContributions.contributeType( new GeolatteGeometryType( SqlServer2008GeometryTypeDescriptor.INSTANCE ) );
		typeContributions.contributeType( new JTSGeometryType( SqlServer2008GeometryTypeDescriptor.INSTANCE ) );
	}

	@Override
	public String getSpatialRelateSQL(String columnName, int spatialRelation) {
		final String stfunction;
		switch ( spatialRelation ) {
			case SpatialRelation.WITHIN:
				stfunction = "STWithin";
				break;
			case SpatialRelation.CONTAINS:
				stfunction = "STContains";
				break;
			case SpatialRelation.CROSSES:
				stfunction = "STCrosses";
				break;
			case SpatialRelation.OVERLAPS:
				stfunction = "STOverlaps";
				break;
			case SpatialRelation.DISJOINT:
				stfunction = "STDisjoint";
				break;
			case SpatialRelation.INTERSECTS:
				stfunction = "STIntersects";
				break;
			case SpatialRelation.TOUCHES:
				stfunction = "STTouches";
				break;
			case SpatialRelation.EQUALS:
				stfunction = "STEquals";
				break;
			default:
				throw new IllegalArgumentException(
						"Spatial relation is not known by this dialect"
				);
		}

		return columnName + "." + stfunction + "(?) = 1";
	}

	@Override
	public String getSpatialFilterExpression(String columnName) {
		return columnName + ".Filter(?) = 1";
	}

	@Override
	public String getSpatialAggregateSQL(String columnName, int aggregation) {
		throw new UnsupportedOperationException( "No spatial aggregate SQL functions." );
	}

	@Override
	public String getDWithinSQL(String columnName) {
		throw new UnsupportedOperationException( "SQL Server has no DWithin function." );
	}

	@Override
	public String getHavingSridSQL(String columnName) {
		return columnName + ".STSrid = (?)";
	}

	@Override
	public String getIsEmptySQL(String columnName, boolean isEmpty) {
		final String base = "(" + columnName + ".STIsEmpty() ";
		return isEmpty ? base + " = 1 )" : base + " = 0 )";
	}

	@Override
	public boolean supportsFiltering() {
		return true;
	}

	@Override
	public boolean supports(SpatialFunction function) {
		return ( getFunctions().get( function.toString() ) != null );
	}
}
