/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial;


import org.hibernate.dialect.Dialect;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkt;

public class GeometryLiteralFormatter<T> implements JdbcLiteralFormatter<T> {

	protected final JavaType<T> javaType;
	protected final Wkt.Dialect wktDialect;
	protected final String geomFromTextName;

	public GeometryLiteralFormatter(JavaType<T> javaType, Wkt.Dialect wktDialect, String geomFromTextName) {
		this.javaType = javaType;
		this.wktDialect = wktDialect;
		this.geomFromTextName = geomFromTextName;
	}

	@Override
	public void appendJdbcLiteral(
			SqlAppender appender, T value, Dialect dialect, WrapperOptions wrapperOptions) {
		Geometry<?> geom = javaType.unwrap( value, Geometry.class, wrapperOptions );
		appender.appendSql( geomFromTextName );
		appender.appendSql("('");
		appender.appendSql( Wkt.toWkt( geom, wktDialect ) );
		appender.appendSql( "'," );
		appender.appendSql( ( Math.max( geom.getSRID(), 0 ) ) );
		appender.appendSql( ")" );
	}

}
