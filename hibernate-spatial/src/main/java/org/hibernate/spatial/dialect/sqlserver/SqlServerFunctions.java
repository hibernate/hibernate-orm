/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.sqlserver;

import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.dialect.SpatialFunctionsRegistry;
import org.hibernate.type.StandardBasicTypes;

/**
 * Created by Karel Maesen, Geovise BVBA on 19/09/2018.
 */
class SqlServerFunctions extends SpatialFunctionsRegistry {
	public SqlServerFunctions() {

		put( "dimension", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "?1.STDimension()" ) );
		put( "geometrytype", new SQLFunctionTemplate( StandardBasicTypes.STRING, "?1.STGeometryType()" ) );
		put( "srid", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "?1.STSrid" ) );
		put( "envelope", new SqlServerMethod( "STEnvelope" ) );
		put( "astext", new SQLFunctionTemplate( StandardBasicTypes.STRING, "?1.STAsText()" ) );
		put( "asbinary", new SQLFunctionTemplate( StandardBasicTypes.BINARY, "?1.STAsBinary()" ) );

		put( "isempty", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.STIsEmpty()" ) );
		put( "issimple", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.STIsSimple()" ) );
		put( "boundary", new SqlServerMethod( "STBoundary" ) );

		// section 2.1.1.2
		// Register functions for spatial relation constructs
		put( "contains", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.STContains(?2)" ) );
		put( "crosses", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.STCrosses(?2)" ) );
		put( "disjoint", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.STDisjoint(?2)" ) );
		put( "equals", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.STEquals(?2)" ) );
		put( "intersects", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.STIntersects(?2)" ) );
		put( "overlaps", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.STOverlaps(?2)" ) );
		put( "touches", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.STTouches(?2)" ) );
		put( "within", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.STWithin(?2)" ) );
		put( "relate", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.STRelate(?2,?3)" ) );

		// section 2.1.1.3
		// Register spatial analysis functions.
		put( "distance", new SQLFunctionTemplate( StandardBasicTypes.DOUBLE, "?1.STDistance(?2)" ) );
		put( "buffer", new SqlServerMethod( "STBuffer" ) );
		put( "convexhull", new SqlServerMethod( "STConvexHull" ) );
		put( "difference", new SqlServerMethod( "STDifference" ) );
		put( "intersection", new SqlServerMethod( "STIntersection" ) );
		put( "symdifference", new SqlServerMethod( "STSymDifference" ) );
		put( "geomunion", new SqlServerMethod( "STUnion" ) );
		// we rename OGC union to geomunion because union is a reserved SQL keyword.
		// (See also postgis documentation).

		// portable spatial aggregate functions
		// no aggregatefunctions implemented in sql-server2000
		//put("extent", new SQLFunctionTemplate(geomType, "?1.STExtent()"));

		// section 2.1.9.1 methods on surfaces
		put( "area", new SQLFunctionTemplate( StandardBasicTypes.DOUBLE, "?1.STArea()" ) );
		put( "centroid", new SqlServerMethod( "STCentroid" ) );
		put(
				"pointonsurface", new SqlServerMethod( "STPointOnSurface" )
		);

		// Register spatial filter function.
		put( SpatialFunction.filter.name(), new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "?1.Filter(?2)" ) );
	}
}
