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

package org.hibernate.spatial.dialect.sqlserver;


import org.hibernate.HibernateException;
import org.hibernate.dialect.SQLServer2008Dialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.spatial.*;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * The <code>SpatialDialect</code> for Microsoft SQL Server (2008).
 *
 * @author Karel Maesen, Martin Steinwender.
 */
public class SqlServer2008SpatialDialect extends SQLServer2008Dialect implements SpatialDialect {

	public final static String SHORT_NAME = "sqlserver";

	public final static String COLUMN_TYPE = "GEOMETRY";

	public SqlServer2008SpatialDialect() {
		super();
		registerColumnType(java.sql.Types.ARRAY, COLUMN_TYPE);

		// registering OGC functions
		// (spec_simplefeatures_sql_99-04.pdf)

		// section 2.1.1.1
		// Registerfunction calls for registering geometry functions:
		// first argument is the OGC standard functionname,
		// second the Function as it occurs in the spatial dialect
		registerFunction("dimension", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "?1.STDimension()"));
		registerFunction("geometrytype", new SQLFunctionTemplate(StandardBasicTypes.STRING, "?1.STGeometryType()"));
		registerFunction("srid", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "?1.STSrid"));
		registerFunction("envelope", new SQLFunctionTemplate(GeometryType.INSTANCE, "?1.STEnvelope()"));
		registerFunction("astext", new SQLFunctionTemplate(StandardBasicTypes.STRING, "?1.STAsText()"));
		registerFunction("asbinary", new SQLFunctionTemplate(StandardBasicTypes.BINARY, "?1.STAsBinary()"));

		registerFunction("isempty", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "?1.STIsEmpty()"));
		registerFunction("issimple", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "?1.STIsSimple()"));
		registerFunction("boundary", new SQLFunctionTemplate(GeometryType.INSTANCE, "?1.STBoundary()"));

		// section 2.1.1.2
		// Register functions for spatial relation constructs
		registerFunction("contains", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "?1.STContains(?2)"));
		registerFunction("crosses", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "?1.STCrosses(?2)"));
		registerFunction("disjoint", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "?1.STDisjoint(?2)"));
		registerFunction("equals", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "?1.STEquals(?2)"));
		registerFunction("intersects", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "?1.STIntersects(?2)"));
		registerFunction("overlaps", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "?1.STOverlaps(?2)"));
		registerFunction("touches", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "?1.STTouches(?2)"));
		registerFunction("within", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "?1.STWithin(?2)"));
		registerFunction("relate", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "?1.STRelate(?2,?3)"));

		// section 2.1.1.3
		// Register spatial analysis functions.
		registerFunction("distance", new SQLFunctionTemplate(StandardBasicTypes.DOUBLE, "?1.STDistance(?2)"));
		registerFunction("buffer", new SQLFunctionTemplate(GeometryType.INSTANCE, "?1.STBuffer(?2)"));
		registerFunction("convexhull", new SQLFunctionTemplate(GeometryType.INSTANCE, "?1.STConvexHull()"));
		registerFunction("difference", new SQLFunctionTemplate(GeometryType.INSTANCE, "?1.STDifference(?2)"));
		registerFunction("intersection", new SQLFunctionTemplate(GeometryType.INSTANCE, "?1.STIntersection(?2)"));
		registerFunction("symdifference", new SQLFunctionTemplate(GeometryType.INSTANCE, "?1.STSymDifference(?2)"));
		registerFunction("geomunion", new SQLFunctionTemplate(GeometryType.INSTANCE, "?1.STUnion(?2)"));
		// we rename OGC union to geomunion because union is a reserved SQL keyword.
		// (See also postgis documentation).

		// portable spatial aggregate functions
		// no aggregatefunctions implemented in sql-server2000
		//registerFunction("extent", new SQLFunctionTemplate(geomType, "?1.STExtent()"));

		// section 2.1.9.1 methods on surfaces
		registerFunction("area", new SQLFunctionTemplate(StandardBasicTypes.DOUBLE, "?1.STArea()"));
		registerFunction("centroid", new SQLFunctionTemplate(GeometryType.INSTANCE, "?1.STCentroid()"));
		registerFunction("pointonsurface", new SQLFunctionTemplate(GeometryType.INSTANCE, "?1.STPointOnSurface()"));
	}

	//Temporary Fix for HHH-6074
	@Override
	public String getTypeName(int code, long length, int precision, int scale) throws HibernateException {
		if (code == 3000) return "GEOMETRY";
		return super.getTypeName(code, length, precision, scale);
	}

	@Override
	public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		if (sqlTypeDescriptor instanceof GeometrySqlTypeDescriptor) {
			return SqlServer2008GeometryTypeDescriptor.INSTANCE;
		}
		return super.remapSqlTypeDescriptor(sqlTypeDescriptor);
	}

	public String getSpatialRelateSQL(String columnName, int spatialRelation) {
		final String stfunction;
		switch (spatialRelation) {
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

	public String getSpatialFilterExpression(String columnName) {
		return columnName + ".Filter(?) = 1";
	}

	public String getSpatialAggregateSQL(String columnName, int aggregation) {
		throw new UnsupportedOperationException("No spatial aggregate SQL functions.");
	}

	public String getDWithinSQL(String columnName) {
		throw new UnsupportedOperationException("SQL Server has no DWithin function.");
	}


	public String getHavingSridSQL(String columnName) {
		return columnName + ".STSrid = (?)";
	}

	public String getIsEmptySQL(String columnName, boolean isEmpty) {
		String base = "(" + columnName + ".STIsEmpty() ";
		return isEmpty ? base + " = 1 )" : base + " = 0 )";
	}

	public boolean supportsFiltering() {
		return true;
	}

	public boolean supports(SpatialFunction function) {
		return (getFunctions().get(function.toString()) != null);
	}
}
