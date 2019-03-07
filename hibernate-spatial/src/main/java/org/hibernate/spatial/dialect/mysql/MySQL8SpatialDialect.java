/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.mysql;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.MySQL8Dialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.GeolatteGeometryJavaTypeDescriptor;
import org.hibernate.spatial.GeolatteGeometryType;
import org.hibernate.spatial.JTSGeometryJavaTypeDescriptor;
import org.hibernate.spatial.JTSGeometryType;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.SpatialRelation;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * Created by Karel Maesen, Geovise BVBA on 2019-03-07.
 */
public class MySQL8SpatialDialect extends MySQL8Dialect implements SpatialDialect {

	private MySQLSpatialDialect dialectDelegate = new MySQLSpatialDialect();
	private MySQL8SpatialFunctions spatialFunctions = new MySQL8SpatialFunctions();

	/**
	 * Constructs an instance
	 */
	public MySQL8SpatialDialect() {
		super();
		registerColumnType(
				MySQLGeometryTypeDescriptor.INSTANCE.getSqlType(),
				"GEOMETRY"
		);
		for ( Map.Entry<String, SQLFunction> entry : spatialFunctions ) {
			registerFunction( entry.getKey(), entry.getValue() );
		}
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		dialectDelegate.contributeTypes( typeContributions, serviceRegistry );
	}

	@Override
	public String getSpatialRelateSQL(String columnName, int spatialRelation) {
		switch ( spatialRelation ) {
			case SpatialRelation.WITHIN:
				return " ST_within(" + columnName + ",?)";
			case SpatialRelation.CONTAINS:
				return " ST_contains(" + columnName + ", ?)";
			case SpatialRelation.CROSSES:
				return " ST_crosses(" + columnName + ", ?)";
			case SpatialRelation.OVERLAPS:
				return " ST_overlaps(" + columnName + ", ?)";
			case SpatialRelation.DISJOINT:
				return " ST_disjoint(" + columnName + ", ?)";
			case SpatialRelation.INTERSECTS:
				return " ST_intersects(" + columnName
						+ ", ?)";
			case SpatialRelation.TOUCHES:
				return " ST_touches(" + columnName + ", ?)";
			case SpatialRelation.EQUALS:
				return " ST_equals(" + columnName + ", ?)";
			default:
				throw new IllegalArgumentException(
						"Spatial relation is not known by this dialect"
				);
		}
	}

	@Override
	public String getTypeName(int code, long length, int precision, int scale) throws HibernateException {
		return dialectDelegate.getTypeName( code, length, precision, scale );
	}

	@Override
	public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		return dialectDelegate.remapSqlTypeDescriptor( sqlTypeDescriptor );
	}

	@Override
	public String getSpatialFilterExpression(String columnName) {
		return dialectDelegate.getSpatialFilterExpression( columnName );
	}

	@Override
	public String getSpatialAggregateSQL(String columnName, int aggregation) {
		return dialectDelegate.getSpatialAggregateSQL( columnName, aggregation );
	}

	@Override
	public String getDWithinSQL(String columnName) {
		return dialectDelegate.getDWithinSQL( columnName );
	}

	@Override
	public String getHavingSridSQL(String columnName) {
		return " (ST_SRID(" + columnName + ") = ?) ";
	}

	@Override
	public String getIsEmptySQL(String columnName, boolean isEmpty) {
		final String emptyExpr = " ST_IsEmpty(" + columnName + ") ";
		return isEmpty ? emptyExpr : "( NOT " + emptyExpr + ")";
	}

	@Override
	public boolean supportsFiltering() {
		return true;
	}

	@Override
	public boolean supports(SpatialFunction function) {
		return spatialFunctions.get( function.toString() ) != null;
	}
}
