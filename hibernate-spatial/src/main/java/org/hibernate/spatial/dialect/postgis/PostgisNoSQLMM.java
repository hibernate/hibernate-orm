/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.postgis;

import org.hibernate.query.sqm.produce.function.spi.StandardSqmFunctionTemplate;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.SpatialRelation;
import org.hibernate.type.StandardBasicTypes;

/**
 *  A Dialect for Postgresql with support for the Postgis spatial types, functions and operators (release 1.x - 1.3)
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Dec 18, 2010
 */
public class PostgisNoSQLMM extends PostgisDialect {

	public PostgisNoSQLMM() {

		registerColumnType(
				PGGeometryTypeDescriptor.INSTANCE.getJdbcTypeCode(),
				"GEOMETRY"
		);

		// registering OGC functions
		// (spec_simplefeatures_sql_99-04.pdf)

		// section 2.1.1.1
		// Registerfunction calls for registering geometry functions:
		// first argument is the OGC standard functionname, second the name as
		// it occurs in the spatial dialect
		registerFunction(
				"dimension", new StandardSqmFunctionTemplate(
				"dimension",
				StandardBasicTypes.INTEGER
		)
		);
		registerFunction(
				"geometrytype", new StandardSqmFunctionTemplate(
				"geometrytype", StandardBasicTypes.STRING
		)
		);
		registerFunction(
				"srid", new StandardSqmFunctionTemplate(
				"srid",
				StandardBasicTypes.INTEGER
		)
		);
		registerFunction(
				"envelope", new StandardSqmFunctionTemplate(
				"envelope"
		)
		);
		registerFunction(
				"astext", new StandardSqmFunctionTemplate(
				"astext",
				StandardBasicTypes.STRING
		)
		);
		registerFunction(
				"asbinary", new StandardSqmFunctionTemplate(
				"asbinary",
				StandardBasicTypes.BINARY
		)
		);
		registerFunction(
				"isempty", new StandardSqmFunctionTemplate(
				"isempty",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"issimple", new StandardSqmFunctionTemplate(
				"issimple",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"boundary", new StandardSqmFunctionTemplate(
				"boundary"
		)
		);

		// Register functions for spatial relation constructs
		registerFunction(
				"overlaps", new StandardSqmFunctionTemplate(
				"overlaps",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"intersects", new StandardSqmFunctionTemplate(
				"intersects",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"equals", new StandardSqmFunctionTemplate(
				"equals",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"contains", new StandardSqmFunctionTemplate(
				"contains",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"crosses", new StandardSqmFunctionTemplate(
				"crosses",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"disjoint", new StandardSqmFunctionTemplate(
				"disjoint",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"touches", new StandardSqmFunctionTemplate(
				"touches",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"within", new StandardSqmFunctionTemplate(
				"within",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"relate", new StandardSqmFunctionTemplate(
				"relate",
				StandardBasicTypes.BOOLEAN
		)
		);

		// register the spatial analysis functions
		registerFunction(
				"distance", new StandardSqmFunctionTemplate(
				"distance",
				StandardBasicTypes.DOUBLE
		)
		);
		registerFunction(
				"buffer", new StandardSqmFunctionTemplate(
				"buffer"
		)
		);
		registerFunction(
				"convexhull", new StandardSqmFunctionTemplate(
				"convexhull"
		)
		);
		registerFunction(
				"difference", new StandardSqmFunctionTemplate(
				"difference"
		)
		);
		registerFunction(
				"intersection", new StandardSqmFunctionTemplate(
				"intersection"
		)
		);
		registerFunction(
				"symdifference",
				new StandardSqmFunctionTemplate( "symdifference" )
		);
		registerFunction(
				"geomunion", new StandardSqmFunctionTemplate(
				"geomunion"
		)
		);

		//register Spatial Aggregate function
		registerFunction(
				"extent", new StandardSqmFunctionTemplate(
				"extent"
		)
		);

		//other common spatial functions
		registerFunction(
				"transform", new StandardSqmFunctionTemplate(
				"transform"
		)
		);
	}

	@Override
	public String getDWithinSQL(String columnName) {
		return "( dwithin(" + columnName + ",?,?) )";
	}

	@Override
	public String getHavingSridSQL(String columnName) {
		return "( srid(" + columnName + ") = ?)";
	}

	@Override
	public String getIsEmptySQL(String columnName, boolean isEmpty) {
		final String emptyExpr = "( isempty(" + columnName + ")) ";
		return isEmpty ? emptyExpr : "not " + emptyExpr;
	}

	@Override
	public String getSpatialRelateSQL(String columnName, int spatialRelation) {
		switch ( spatialRelation ) {
			case SpatialRelation.WITHIN:
				return "(" + columnName + " && ?  AND within(" + columnName + ", ?))";
			case SpatialRelation.CONTAINS:
				return "(" + columnName + " && ? AND contains(" + columnName + ", ?))";
			case SpatialRelation.CROSSES:
				return "(" + columnName + " && ? AND crosses(" + columnName + ", ?))";
			case SpatialRelation.OVERLAPS:
				return "(" + columnName + " && ? AND overlaps(" + columnName + ", ?))";
			case SpatialRelation.DISJOINT:
				return "(" + columnName + " && ? AND disjoint(" + columnName + ", ?))";
			case SpatialRelation.INTERSECTS:
				return "(" + columnName + " && ? AND intersects(" + columnName + ", ?))";
			case SpatialRelation.TOUCHES:
				return "(" + columnName + " && ? AND touches(" + columnName + ", ?))";
			case SpatialRelation.EQUALS:
				return "(" + columnName + " && ? AND equals(" + columnName + ", ?))";
			default:
				throw new IllegalArgumentException( "Spatial relation is not known by this dialect" );
		}

	}

	@Override
	public boolean supports(SpatialFunction function) {
		return super.supports( function );
	}
}
