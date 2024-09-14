/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.oracle;

import org.hibernate.dialect.Dialect;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkt;

class OracleJdbcLiteralFormatter<T> implements JdbcLiteralFormatter<T> {

	private final JavaType<T> javaType;

	OracleJdbcLiteralFormatter(JavaType<T> javaType) {
		this.javaType = javaType;
	}

	@Override
	public void appendJdbcLiteral(
			SqlAppender appender, T value, Dialect dialect, WrapperOptions wrapperOptions) {
		Geometry<?> geom = javaType.unwrap( value, Geometry.class, wrapperOptions );
		appender.appendSql( "ST_GEOMETRY.FROM_WKT" );
		appender.appendSql( "('" );
		appender.appendSql( Wkt.toWkt( geom, Wkt.Dialect.SFA_1_2_1 ) );
		appender.appendSql( "'," );
		appender.appendSql( ( Math.max( geom.getSRID(), 0 ) ) );
		appender.appendSql( ").Geom" );
	}
}
