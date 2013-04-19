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

import java.util.Map;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.metamodel.spi.TypeContributions;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.GeolatteGeometryType;
import org.hibernate.spatial.JTSGeometryType;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.SpatialRelation;

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
		registerColumnType(
				MySQLGeometryTypeDescriptor.INSTANCE.getSqlType(),
				"GEOMETRY"
		);
		for ( Map.Entry<String, StandardSQLFunction> entry : new MySQLSpatialFunctions() ) {
			registerFunction( entry.getKey(), entry.getValue() );
		}
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(
				typeContributions,
				serviceRegistry
		);
		typeContributions.contributeType( new GeolatteGeometryType( MySQLGeometryTypeDescriptor.INSTANCE ) );
		typeContributions.contributeType( new JTSGeometryType( MySQLGeometryTypeDescriptor.INSTANCE ) );
	}

	/**
	 * @param columnName The name of the geometry-typed column to which the relation is
	 * applied
	 * @param spatialRelation The type of spatial relation (as defined in
	 * <code>SpatialRelation</code>).
	 *
	 * @return
	 */
	public String getSpatialRelateSQL(String columnName, int spatialRelation) {
		switch ( spatialRelation ) {
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
						"Spatial relation is not known by this dialect"
				);
		}

	}

	public String getSpatialFilterExpression(String columnName) {
		return "MBRIntersects(" + columnName + ", ? ) ";
	}

	public String getSpatialAggregateSQL(String columnName, int aggregation) {
		throw new UnsupportedOperationException( "Mysql has no spatial aggregate SQL functions." );
	}

	public String getDWithinSQL(String columnName) {
		throw new UnsupportedOperationException( String.format( "Mysql doesn't support the Dwithin function" ) );
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
		switch ( function ) {
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
