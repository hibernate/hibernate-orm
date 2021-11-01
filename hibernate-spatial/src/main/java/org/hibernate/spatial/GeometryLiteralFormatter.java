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
import org.geolatte.geom.jts.JTS;

public class GeometryLiteralFormatter<T> implements JdbcLiteralFormatter<T> {

	private final JavaType<T> javaType;
	private final Wkt.Dialect wktDialect;
	private final String geomFromTextName;

	public GeometryLiteralFormatter(JavaType<T> javaType, Wkt.Dialect wktDialect, String geomFromTextName) {
		this.javaType = javaType;
		this.wktDialect = wktDialect;
		this.geomFromTextName = geomFromTextName;
	}

	@Override
	//todo -- clean this up
	public void appendJdbcLiteral(
			SqlAppender appender, T value, Dialect dialect, WrapperOptions wrapperOptions) {
		appender.appendSql( geomFromTextName );
		int srid = 0;
		appender.appendSql( "('" );
		if ( javaType instanceof GeolatteGeometryJavaTypeDescriptor ) {
			appender.appendSql( Wkt.toWkt( (Geometry<?>) value, wktDialect ) );
			srid = ( (Geometry<?>) value ).getSRID();
		}
		else {
			appender.appendSql( Wkt.toWkt( jts2Gl( value ), wktDialect ) );
			srid = ( (org.locationtech.jts.geom.Geometry) value ).getSRID();
		}
		appender.appendSql( "', " );
		appender.appendSql( srid );
		appender.appendSql(")");
	}

	private <T> Geometry<?> jts2Gl(T value) {
		return JTS.from( (org.locationtech.jts.geom.Geometry) value );
	}

}
