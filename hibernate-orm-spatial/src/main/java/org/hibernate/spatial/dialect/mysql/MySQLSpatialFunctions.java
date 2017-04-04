/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.mysql;

import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.spatial.dialect.SpatialFunctionsRegistry;
import org.hibernate.type.StandardBasicTypes;

/**
 * An {@code Iterable} over the spatial functions supported by MySQL.
 *
 * @author Karel Maesen, Geovise BVBA
 *
 */
class MySQLSpatialFunctions extends SpatialFunctionsRegistry {

	MySQLSpatialFunctions(){
		functionMap.put(
				"dimension", new StandardSQLFunction(
				"dimension",
				StandardBasicTypes.INTEGER
		)
		);
		functionMap.put(
				"geometrytype", new StandardSQLFunction(
				"geometrytype", StandardBasicTypes.STRING
		)
		);
		functionMap.put(
				"srid", new StandardSQLFunction(
				"srid",
				StandardBasicTypes.INTEGER
		)
		);
		functionMap.put(
				"envelope", new StandardSQLFunction(
				"envelope"
		)
		);
		functionMap.put(
				"astext", new StandardSQLFunction(
				"astext",
				StandardBasicTypes.STRING
		)
		);
		functionMap.put(
				"asbinary", new StandardSQLFunction(
				"asbinary",
				StandardBasicTypes.BINARY
		)
		);
		functionMap.put(
				"isempty", new StandardSQLFunction(
				"isempty",
				StandardBasicTypes.BOOLEAN
		)
		);
		functionMap.put(
				"issimple", new StandardSQLFunction(
				"issimple",
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
				"overlaps",
				StandardBasicTypes.BOOLEAN
		)
		);
		functionMap.put(
				"intersects", new StandardSQLFunction(
				"intersects",
				StandardBasicTypes.BOOLEAN
		)
		);
		functionMap.put(
				"equals", new StandardSQLFunction(
				"equals",
				StandardBasicTypes.BOOLEAN
		)
		);
		functionMap.put(
				"contains", new StandardSQLFunction(
				"contains",
				StandardBasicTypes.BOOLEAN
		)
		);
		functionMap.put(
				"crosses", new StandardSQLFunction(
				"crosses",
				StandardBasicTypes.BOOLEAN
		)
		);
		functionMap.put(
				"disjoint", new StandardSQLFunction(
				"disjoint",
				StandardBasicTypes.BOOLEAN
		)
		);
		functionMap.put(
				"touches", new StandardSQLFunction(
				"touches",
				StandardBasicTypes.BOOLEAN
		)
		);
		functionMap.put(
				"within", new StandardSQLFunction(
				"within",
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
//		// register the spatial analysis functions
//		functionMap.put(
//				"distance", new StandardSQLFunction(
//				"distance",
//				StandardBasicTypes.DOUBLE
//		)
//		);
//		functionMap.put(
//				"buffer", new StandardSQLFunction(
//				"buffer"
//		)
//		);
//		functionMap.put(
//				"convexhull", new StandardSQLFunction(
//				"convexhull"
//		)
//		);
//		functionMap.put(
//				"difference", new StandardSQLFunction(
//				"difference"
//		)
//		);
//		functionMap.put(
//				"intersection", new StandardSQLFunction(
//				"intersection"
//		)
//		);
//		functionMap.put(
//				"symdifference", new StandardSQLFunction(
//				"symdifference"
//		)
//		);
//		functionMap.put(
//				"geomunion", new StandardSQLFunction(
//				"union"
//		)
//		);
	}

}
