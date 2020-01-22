/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.mysql;

import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.dialect.SpatialFunctionsRegistry;
import org.hibernate.type.StandardBasicTypes;

/**
 * An {@code Iterable} over the spatial functions supported by MySQL 8.
 *
 * @author Karel Maesen, Geovise BVBA
 */
class MySQL8SpatialFunctions extends SpatialFunctionsRegistry {

	MySQL8SpatialFunctions() {
		functionMap.put(
				"dimension", new StandardSQLFunction(
						"ST_Dimension",
						StandardBasicTypes.INTEGER
				)
		);
		functionMap.put(
				"geometrytype", new StandardSQLFunction(
						"ST_GeometryType", StandardBasicTypes.STRING
				)
		);
		functionMap.put(
				"srid", new StandardSQLFunction(
						"ST_SRID",
						StandardBasicTypes.INTEGER
				)
		);
		functionMap.put(
				"envelope", new StandardSQLFunction(
						"ST_Envelope"
				)
		);
		functionMap.put(
				"astext", new StandardSQLFunction(
						"ST_Astext",
						StandardBasicTypes.STRING
				)
		);
		functionMap.put(
				"asbinary", new StandardSQLFunction(
						"ST_Asbinary",
						StandardBasicTypes.BINARY
				)
		);
		functionMap.put(
				"isempty", new StandardSQLFunction(
						"ST_IsEmpty",
						StandardBasicTypes.BOOLEAN
				)
		);
		functionMap.put(
				"issimple", new StandardSQLFunction(
						"ST_IsSimple",
						StandardBasicTypes.BOOLEAN
				)
		);
//		functionMap.put(
//				"boundary", new StandardSQLFunction(
//				"boundary"
//		)
//		);

		// Register functions for spatial relation constructs
		functionMap.put(
				"overlaps", new StandardSQLFunction(
						"ST_Overlaps",
						StandardBasicTypes.BOOLEAN
				)
		);
		functionMap.put(
				"intersects", new StandardSQLFunction(
						"ST_Intersects",
						StandardBasicTypes.BOOLEAN
				)
		);
		functionMap.put(
				"equals", new StandardSQLFunction(
						"ST_Equals",
						StandardBasicTypes.BOOLEAN
				)
		);
		functionMap.put(
				"contains", new StandardSQLFunction(
						"ST_Contains",
						StandardBasicTypes.BOOLEAN
				)
		);
		functionMap.put(
				"crosses", new StandardSQLFunction(
						"ST_Crosses",
						StandardBasicTypes.BOOLEAN
				)
		);
		functionMap.put(
				"disjoint", new StandardSQLFunction(
						"ST_Disjoint",
						StandardBasicTypes.BOOLEAN
				)
		);
		functionMap.put(
				"touches", new StandardSQLFunction(
						"ST_Touches",
						StandardBasicTypes.BOOLEAN
				)
		);
		functionMap.put(
				"within", new StandardSQLFunction(
						"ST_Within",
						StandardBasicTypes.BOOLEAN
				)
		);
//		functionMap.put(
//				"relate", new StandardSQLFunction(
//				"relate",
//				StandardBasicTypes.BOOLEAN
//		)
//		);
//
		// register the spatial analysis functions
		functionMap.put(
				"distance", new StandardSQLFunction(
						"ST_Distance",
						StandardBasicTypes.DOUBLE
				)
		);

		functionMap.put(
				"buffer", new StandardSQLFunction(
						"ST_Buffer"
				)
		);

		functionMap.put(
				"convexhull", new StandardSQLFunction(
						"ST_ConvexHull"
				)
		);

		functionMap.put(
				"difference", new StandardSQLFunction(
						"ST_Difference"
				)
		);

		functionMap.put(
				"intersection", new StandardSQLFunction(
						"ST_Intersection"
				)
		);

		functionMap.put(
				"symdifference", new StandardSQLFunction(
						"ST_SymDifference"
				)
		);

		functionMap.put(
				"geomunion", new StandardSQLFunction(
						"ST_Union"
				)
		);

		functionMap.put(
				SpatialFunction.filter.name(), new StandardSQLFunction(
						"MBRIntersects",
						StandardBasicTypes.BOOLEAN
				)
		);
	}

}
