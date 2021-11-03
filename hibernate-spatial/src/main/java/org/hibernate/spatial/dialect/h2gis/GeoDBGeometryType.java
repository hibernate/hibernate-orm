/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.h2gis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.spatial.GeolatteGeometryJavaTypeDescriptor;
import org.hibernate.spatial.GeometryLiteralFormatter;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.jts.JTS;

/**
 * Descriptor for GeoDB Geometries.
 *
 * @author Karel Maesen, Geovise BVBA
 */
public class GeoDBGeometryType implements JdbcType {

	/**
	 * An instance of this Descriptor
	 */
	public static final GeoDBGeometryType INSTANCE = new GeoDBGeometryType();

	@Override
	public int getJdbcTypeCode() {
		return Types.ARRAY;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.GEOMETRY;
	}

	//todo -- simplify as with postgis/mariadb
	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaTypeDescriptor) {
		return new GeometryLiteralFormatter<T>( javaTypeDescriptor, Wkt.Dialect.SFA_1_1_0, "ST_GeomFromText" );
	}

	private <T> Geometry<?> jts2Gl(T value) {
		return JTS.from( (org.locationtech.jts.geom.Geometry) value );
	}


	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		return new BasicBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final Geometry geometry = getJavaTypeDescriptor().unwrap( value, Geometry.class, options );
				st.setBytes( index, GeoDbWkb.to( geometry ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final Geometry geometry = getJavaTypeDescriptor().unwrap( value, Geometry.class, options );
				st.setBytes( name, GeoDbWkb.to( geometry ) );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {

			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getJavaTypeDescriptor().wrap( GeoDbWkb.from( rs.getObject( paramIndex ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getJavaTypeDescriptor().wrap( GeoDbWkb.from( statement.getObject( index ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getJavaTypeDescriptor().wrap( GeoDbWkb.from( statement.getObject( name ) ), options );
			}
		};
	}


}
