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
package org.hibernate.spatial.dialect.mysql;

import org.hibernate.HibernateException;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.spatial.*;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Extends the MySQLDialect by also including information on spatial operators,
 * constructors and processing functions.
 *
 * @author Karel Maesen
 * @author Boni Gopalan [3/11/2011:Refactored the code to introduce MySQLSpatialInnoDBDialect without much code duplication]
 */
public class MySQLSpatialDialect extends MySQLDialect implements SpatialDialect {

	public MySQLSpatialDialect() {
		super();
		Map<String, StandardSQLFunction> functionsToRegister = getFunctionsToRegister();
		Map<String, Integer> columnTypes = getColumnTypesToRegister();
		if (null != columnTypes) {
			Iterator<String> keys = columnTypes.keySet().iterator();
			while (keys.hasNext()) {
				String aKey = keys.next();
				registerColumnType(columnTypes.get(aKey), aKey);
			}
		}

		if (null != functionsToRegister) {
			Iterator<String> keys = functionsToRegister.keySet().iterator();
			while (keys.hasNext()) {
				String aKey = keys.next();
				registerFunction(aKey, functionsToRegister.get(aKey));

			}
		}
	}

	protected Map<String, Integer> getColumnTypesToRegister() {
		Map<String, Integer> columnTypes = new HashMap<String, Integer>();
		columnTypes.put("GEOMETRY", java.sql.Types.ARRAY);
		return columnTypes;
	}

	protected Map<String, StandardSQLFunction> getFunctionsToRegister() {
		Map<String, StandardSQLFunction> functionsToRegister = new HashMap<String, StandardSQLFunction>();

		// registering OGC functions
		// (spec_simplefeatures_sql_99-04.pdf)

		// section 2.1.1.1
		// Registerfunction calls for registering geometry functions:
		// first argument is the OGC standard functionname, second the name as
		// it occurs in the spatial dialect

		functionsToRegister.put("dimension", new StandardSQLFunction("dimension",
				StandardBasicTypes.INTEGER));
		functionsToRegister.put("geometrytype", new StandardSQLFunction(
				"geometrytype", StandardBasicTypes.STRING));
		functionsToRegister.put("srid", new StandardSQLFunction("srid",
				StandardBasicTypes.INTEGER));
		functionsToRegister.put("envelope", new StandardSQLFunction("envelope",
				GeometryType.INSTANCE));
		functionsToRegister.put("astext", new StandardSQLFunction("astext",
				StandardBasicTypes.STRING));
		functionsToRegister.put("asbinary", new StandardSQLFunction("asbinary",
				StandardBasicTypes.BINARY));
		functionsToRegister.put("isempty", new StandardSQLFunction("isempty",
				StandardBasicTypes.BOOLEAN));
		functionsToRegister.put("issimple", new StandardSQLFunction("issimple",
				StandardBasicTypes.BOOLEAN));
		functionsToRegister.put("boundary", new StandardSQLFunction("boundary",
				GeometryType.INSTANCE));

		// Register functions for spatial relation constructs
		functionsToRegister.put("overlaps", new StandardSQLFunction("overlaps",
				StandardBasicTypes.BOOLEAN));
		functionsToRegister.put("intersects", new StandardSQLFunction("intersects",
				StandardBasicTypes.BOOLEAN));
		functionsToRegister.put("equals", new StandardSQLFunction("equals",
				StandardBasicTypes.BOOLEAN));
		functionsToRegister.put("contains", new StandardSQLFunction("contains",
				StandardBasicTypes.BOOLEAN));
		functionsToRegister.put("crosses", new StandardSQLFunction("crosses",
				StandardBasicTypes.BOOLEAN));
		functionsToRegister.put("disjoint", new StandardSQLFunction("disjoint",
				StandardBasicTypes.BOOLEAN));
		functionsToRegister.put("touches", new StandardSQLFunction("touches",
				StandardBasicTypes.BOOLEAN));
		functionsToRegister.put("within", new StandardSQLFunction("within",
				StandardBasicTypes.BOOLEAN));
		functionsToRegister.put("relate", new StandardSQLFunction("relate",
				StandardBasicTypes.BOOLEAN));

		// register the spatial analysis functions
		functionsToRegister.put("distance", new StandardSQLFunction("distance",
				StandardBasicTypes.DOUBLE));
		functionsToRegister.put("buffer", new StandardSQLFunction("buffer",
				GeometryType.INSTANCE));
		functionsToRegister.put("convexhull", new StandardSQLFunction("convexhull",
				GeometryType.INSTANCE));
		functionsToRegister.put("difference", new StandardSQLFunction("difference",
				GeometryType.INSTANCE));
		functionsToRegister.put("intersection", new StandardSQLFunction(
				"intersection", GeometryType.INSTANCE));
		functionsToRegister.put("symdifference", new StandardSQLFunction(
				"symdifference", GeometryType.INSTANCE));
		functionsToRegister.put("geomunion", new StandardSQLFunction("union",
				GeometryType.INSTANCE));
		return functionsToRegister;
	}


	//TODO the getTypeName() override is necessary in the absence of HHH-6074
	@Override
	public String getTypeName(int code, long length, int precision, int scale) throws HibernateException {
		if (code == 3000) return "GEOMETRY";
		return super.getTypeName(code, length, precision, scale);
	}

	@Override
	public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		if (sqlTypeDescriptor instanceof GeometrySqlTypeDescriptor) {
			return MySQLGeometryTypeDescriptor.INSTANCE;
		}
		return super.remapSqlTypeDescriptor(sqlTypeDescriptor);
	}

	/**
	 * @param columnName	  The name of the geometry-typed column to which the relation is
	 *                        applied
	 * @param spatialRelation The type of spatial relation (as defined in
	 *                        <code>SpatialRelation</code>).
	 * @return
	 */
	public String getSpatialRelateSQL(String columnName, int spatialRelation) {
		switch (spatialRelation) {
			case SpatialRelation.WITHIN:
				return " within(" + columnName + ",?)";
			case SpatialRelation.CONTAINS:
				return " contains(" + columnName + ", ?)";
			case SpatialRelation.CROSSES:
				return " crosses(" + columnName + ", ?)";
			case SpatialRelation.OVERLAPS:
				return " overlaps(" + columnName + ", ?)";
			case SpatialRelation.DISJOINT:
				return " disjoint(" + columnName + ", ?)";
			case SpatialRelation.INTERSECTS:
				return " intersects(" + columnName + ", ?)";
			case SpatialRelation.TOUCHES:
				return " touches(" + columnName + ", ?)";
			case SpatialRelation.EQUALS:
				return " equals(" + columnName + ", ?)";
			default:
				throw new IllegalArgumentException(
						"Spatial relation is not known by this dialect");
		}

	}

	public String getSpatialFilterExpression(String columnName) {
		return "MBRIntersects(" + columnName + ", ? ) ";
	}

	public String getSpatialAggregateSQL(String columnName, int aggregation) {
		throw new UnsupportedOperationException("Mysql has no spatial aggregate SQL functions.");
	}

	public String getDWithinSQL(String columnName) {
		throw new UnsupportedOperationException(String.format("Mysql doesn't support the Dwithin function"));
	}

	public String getHavingSridSQL(String columnName) {
		return " (srid(" + columnName + ") = ?) ";
	}

	public String getIsEmptySQL(String columnName, boolean isEmpty) {
		String emptyExpr = " IsEmpty(" + columnName + ") ";
		return isEmpty ? emptyExpr : "( NOT " + emptyExpr + ")";
	}

	public String getDbGeometryTypeName() {
		return "GEOMETRY";
	}

	public boolean supportsFiltering() {
		return false;
	}

	public boolean supports(SpatialFunction function) {
		switch (function) {
			case boundary:
			case relate:
			case distance:
			case buffer:
			case convexhull:
			case difference:
			case symdifference:
			case intersection:
			case geomunion:
			case dwithin:
			case transform:
				return false;
		}
		return true;
	}

}
