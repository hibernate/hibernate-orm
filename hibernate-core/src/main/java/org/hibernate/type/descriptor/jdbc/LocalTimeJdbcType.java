/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import jakarta.persistence.TemporalType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterTemporal;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalTime;

/**
 * Descriptor for {@link Types#TIME TIME} handling.
 *
 * @author Steve Ebersole
 */
public class LocalTimeJdbcType implements JdbcType {
	public static final LocalTimeJdbcType INSTANCE = new LocalTimeJdbcType();

	public LocalTimeJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.TIME;
	}

	@Override
	public String getFriendlyName() {
		return "LOCAL_TIME";
	}

	@Override
	public String toString() {
		return "LocalTimeTypeDescriptor";
	}

	@Override
	public <T> BasicJavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return (BasicJavaType<T>) typeConfiguration.getJavaTypeRegistry().getDescriptor( LocalTime.class );
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterTemporal<>( javaType, TemporalType.TIME );
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return LocalTime.class;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				final LocalTime time = javaType.unwrap( value, LocalTime.class, options );
				try {
					st.setObject( index, time, Types.TIME );
				}
				catch (SQLDataException sde) {
					st.setObject( index, time );
				}
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
				final LocalTime time = javaType.unwrap( value, LocalTime.class, options );
				try {
					st.setObject( name, time, Types.TIME );
				}
				catch (SQLDataException sde) {
					st.setObject( name, time );
				}
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return javaType.wrap( rs.getObject( paramIndex, LocalTime.class ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaType.wrap( statement.getObject( index, LocalTime.class ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return javaType.wrap( statement.getObject( name, LocalTime.class ), options );
			}
		};
	}
}
