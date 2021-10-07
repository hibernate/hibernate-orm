/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterNumericData;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#FLOAT FLOAT} handling.
 *
 * @author Steve Ebersole
 */
public class FloatJdbcTypeDescriptor implements JdbcTypeDescriptor {
	public static final FloatJdbcTypeDescriptor INSTANCE = new FloatJdbcTypeDescriptor();

	public FloatJdbcTypeDescriptor() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.FLOAT;
	}

	@Override
	public String getFriendlyName() {
		return "FLOAT";
	}

	@Override
	public String toString() {
		return "FloatTypeDescriptor";
	}

	@Override
	public <T> BasicJavaTypeDescriptor<T> getJdbcRecommendedJavaTypeMapping(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		if ( length != null && length <= typeConfiguration.getServiceRegistry().getService( JdbcServices.class ).getDialect().getFloatPrecision() ) {
			return (BasicJavaTypeDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Float.class );
		}
		return (BasicJavaTypeDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Double.class );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
		return new JdbcLiteralFormatterNumericData( javaTypeDescriptor, Float.class );
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				st.setFloat( index, javaTypeDescriptor.unwrap( value, Float.class, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setFloat( name, javaTypeDescriptor.unwrap( value, Float.class, options ) );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( rs.getFloat( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getFloat( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getFloat( name ), options );
			}
		};
	}
}
