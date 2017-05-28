/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.mysql;

import org.hibernate.query.sqm.produce.function.spi.NamedSqmFunctionTemplate;
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
				"dimension", new NamedSqmFunctionTemplate(
				"dimension",
				StandardBasicTypes.INTEGER
		)
		);
		functionMap.put(
				"geometrytype", new NamedSqmFunctionTemplate(
				"geometrytype", StandardBasicTypes.STRING
		)
		);
		functionMap.put(
				"srid", new NamedSqmFunctionTemplate(
				"srid",
				StandardBasicTypes.INTEGER
		)
		);
		functionMap.put(
				"envelope", new NamedSqmFunctionTemplate(
				"envelope"
		)
		);
		functionMap.put(
				"astext", new NamedSqmFunctionTemplate(
				"astext",
				StandardBasicTypes.STRING
		)
		);
		functionMap.put(
				"asbinary", new NamedSqmFunctionTemplate(
				"asbinary",
				StandardBasicTypes.BINARY
		)
		);
		functionMap.put(
				"isempty", new NamedSqmFunctionTemplate(
				"isempty",
				StandardBasicTypes.BOOLEAN
		)
		);
		functionMap.put(
				"issimple", new NamedSqmFunctionTemplate(
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
				"overlaps", new NamedSqmFunctionTemplate(
				"overlaps",
				StandardBasicTypes.BOOLEAN
		)
		);
		functionMap.put(
				"intersects", new NamedSqmFunctionTemplate(
				"intersects",
				StandardBasicTypes.BOOLEAN
		)
		);
		functionMap.put(
				"equals", new NamedSqmFunctionTemplate(
				"equals",
				StandardBasicTypes.BOOLEAN
		)
		);
		functionMap.put(
				"contains", new NamedSqmFunctionTemplate(
				"contains",
				StandardBasicTypes.BOOLEAN
		)
		);
		functionMap.put(
				"crosses", new NamedSqmFunctionTemplate(
				"crosses",
				StandardBasicTypes.BOOLEAN
		)
		);
		functionMap.put(
				"disjoint", new NamedSqmFunctionTemplate(
				"disjoint",
				StandardBasicTypes.BOOLEAN
		)
		);
		functionMap.put(
				"touches", new NamedSqmFunctionTemplate(
				"touches",
				StandardBasicTypes.BOOLEAN
		)
		);
		functionMap.put(
				"within", new NamedSqmFunctionTemplate(
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
