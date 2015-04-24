/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.spatial.dialect.mysql;

import java.util.Map;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.function.StandardSQLFunction;

import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.GeolatteGeometryType;
import org.hibernate.spatial.JTSGeometryType;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.SpatialRelation;

/**
 * A Dialect for MySQL with support for its spatial features
 *
 * @author Karel Maesen, Boni Gopalan
 */
public class MySQLSpatialDialect extends MySQLDialect implements SpatialDialect {

	/**
	 * Constructs an instance
	 */
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

	@Override
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

	@Override
	public String getSpatialFilterExpression(String columnName) {
		return "MBRIntersects(" + columnName + ", ? ) ";
	}

	@Override
	public String getSpatialAggregateSQL(String columnName, int aggregation) {
		throw new UnsupportedOperationException( "Mysql has no spatial aggregate SQL functions." );
	}

	@Override
	public String getDWithinSQL(String columnName) {
		throw new UnsupportedOperationException( String.format( "Mysql doesn't support the Dwithin function" ) );
	}

	@Override
	public String getHavingSridSQL(String columnName) {
		return " (srid(" + columnName + ") = ?) ";
	}

	@Override
	public String getIsEmptySQL(String columnName, boolean isEmpty) {
		final String emptyExpr = " IsEmpty(" + columnName + ") ";
		return isEmpty ? emptyExpr : "( NOT " + emptyExpr + ")";
	}

	@Override
	public boolean supportsFiltering() {
		return false;
	}

	@Override
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
			default:
				return true;
		}
	}

}
