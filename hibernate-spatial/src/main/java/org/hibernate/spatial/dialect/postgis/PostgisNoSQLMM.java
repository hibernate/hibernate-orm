/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.hibernate.spatial.dialect.postgis;

import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.spatial.JTSGeometryType;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.SpatialRelation;
import org.hibernate.type.StandardBasicTypes;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Dec 18, 2010
 */
public class PostgisNoSQLMM extends PostgisDialect {
	@Override
	protected void registerTypesAndFunctions() {

		registerColumnType(
				PGGeometryTypeDescriptor.INSTANCE.getSqlType(),
				PGGeometryTypeDescriptor.INSTANCE.getTypeName()
		);

		// registering OGC functions
		// (spec_simplefeatures_sql_99-04.pdf)

		// section 2.1.1.1
		// Registerfunction calls for registering geometry functions:
		// first argument is the OGC standard functionname, second the name as
		// it occurs in the spatial dialect
		registerFunction(
				"dimension", new StandardSQLFunction(
				"dimension",
				StandardBasicTypes.INTEGER
		)
		);
		registerFunction(
				"geometrytype", new StandardSQLFunction(
				"geometrytype", StandardBasicTypes.STRING
		)
		);
		registerFunction(
				"srid", new StandardSQLFunction(
				"srid",
				StandardBasicTypes.INTEGER
		)
		);
		registerFunction(
				"envelope", new StandardSQLFunction(
				"envelope",
				new JTSGeometryType()
		)
		);
		registerFunction(
				"astext", new StandardSQLFunction(
				"astext",
				StandardBasicTypes.STRING
		)
		);
		registerFunction(
				"asbinary", new StandardSQLFunction(
				"asbinary",
				StandardBasicTypes.BINARY
		)
		);
		registerFunction(
				"isempty", new StandardSQLFunction(
				"isempty",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"issimple", new StandardSQLFunction(
				"issimple",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"boundary", new StandardSQLFunction(
				"boundary",
				new JTSGeometryType()
		)
		);

		// Register functions for spatial relation constructs
		registerFunction(
				"overlaps", new StandardSQLFunction(
				"overlaps",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"intersects", new StandardSQLFunction(
				"intersects",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"equals", new StandardSQLFunction(
				"equals",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"contains", new StandardSQLFunction(
				"contains",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"crosses", new StandardSQLFunction(
				"crosses",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"disjoint", new StandardSQLFunction(
				"disjoint",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"touches", new StandardSQLFunction(
				"touches",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"within", new StandardSQLFunction(
				"within",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"relate", new StandardSQLFunction(
				"relate",
				StandardBasicTypes.BOOLEAN
		)
		);

		// register the spatial analysis functions
		registerFunction(
				"distance", new StandardSQLFunction(
				"distance",
				StandardBasicTypes.DOUBLE
		)
		);
		registerFunction(
				"buffer", new StandardSQLFunction(
				"buffer",
				JTSGeometryType.INSTANCE
		)
		);
		registerFunction(
				"convexhull", new StandardSQLFunction(
				"convexhull",
				JTSGeometryType.INSTANCE
		)
		);
		registerFunction(
				"difference", new StandardSQLFunction(
				"difference",
				JTSGeometryType.INSTANCE
		)
		);
		registerFunction(
				"intersection", new StandardSQLFunction(
				"intersection", new JTSGeometryType()
		)
		);
		registerFunction(
				"symdifference",
				new StandardSQLFunction( "symdifference", JTSGeometryType.INSTANCE )
		);
		registerFunction(
				"geomunion", new StandardSQLFunction(
				"geomunion",
				JTSGeometryType.INSTANCE
		)
		);

		//register Spatial Aggregate function
		registerFunction(
				"extent", new StandardSQLFunction(
				"extent",
				JTSGeometryType.INSTANCE
		)
		);

		//other common spatial functions
		registerFunction(
				"transform", new StandardSQLFunction(
				"transform",
				JTSGeometryType.INSTANCE
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
		String emptyExpr = "( isempty(" + columnName + ")) ";
		return isEmpty ? emptyExpr : "not " + emptyExpr;
	}

	public String getSpatialRelateSQL(String columnName, int spatialRelation,
									  boolean hasFilter) {
		switch ( spatialRelation ) {
			case SpatialRelation.WITHIN:
				return hasFilter ? "(" + columnName + " && ?  AND   within("
						+ columnName + ", ?))" : " within(" + columnName + ",?)";
			case SpatialRelation.CONTAINS:
				return hasFilter ? "(" + columnName + " && ? AND contains("
						+ columnName + ", ?))" : " contains(" + columnName + ", ?)";
			case SpatialRelation.CROSSES:
				return hasFilter ? "(" + columnName + " && ? AND crosses("
						+ columnName + ", ?))" : " crosses(" + columnName + ", ?)";
			case SpatialRelation.OVERLAPS:
				return hasFilter ? "(" + columnName + " && ? AND overlaps("
						+ columnName + ", ?))" : " overlaps(" + columnName + ", ?)";
			case SpatialRelation.DISJOINT:
				return hasFilter ? "(" + columnName + " && ? AND disjoint("
						+ columnName + ", ?))" : " disjoint(" + columnName + ", ?)";
			case SpatialRelation.INTERSECTS:
				return hasFilter ? "(" + columnName + " && ? AND intersects("
						+ columnName + ", ?))" : " intersects(" + columnName
						+ ", ?)";
			case SpatialRelation.TOUCHES:
				return hasFilter ? "(" + columnName + " && ? AND touches("
						+ columnName + ", ?))" : " touches(" + columnName + ", ?)";
			case SpatialRelation.EQUALS:
				return hasFilter ? "(" + columnName + " && ? AND equals("
						+ columnName + ", ?))" : " equals(" + columnName + ", ?)";
			default:
				throw new IllegalArgumentException(
						"Spatial relation is not known by this dialect"
				);
		}

	}

	@Override
	public boolean supports(SpatialFunction function) {
		return super.supports( function );
	}
}
