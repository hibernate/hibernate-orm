/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.sqlserver;

import org.hibernate.spatial.GeometryLiteralFormatter;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;

import org.geolatte.geom.codec.Wkt;

/**
 * Type descriptor for the SQL Server 2008 Geometry type.
 *
 * @author Karel Maesen, Geovise BVBA
 * creation-date: 8/23/11
 */
public class SqlServerGeometryType extends AbstractSqlServerGISType {

	/**
	 * An instance of the descrtiptor
	 */
	public static final SqlServerGeometryType INSTANCE = new SqlServerGeometryType();

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.GEOMETRY;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new GeometryLiteralFormatter<>( javaType, Wkt.Dialect.SFA_1_2_1, "geometry::STGeomFromText" );
	}

}
