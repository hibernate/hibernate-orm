/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.sql;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.descriptor.spi.ValueBinder;
import org.hibernate.type.descriptor.spi.ValueExtractor;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.internal.JdbcLiteralFormatterNumericData;

/**
 * Descriptor for {@link Types#INTEGER INTEGER} handling.
 *
 * @author Steve Ebersole
 */
public class IntegerTypeDescriptor implements SqlTypeDescriptor {
	public static final IntegerTypeDescriptor INSTANCE = new IntegerTypeDescriptor();

	public IntegerTypeDescriptor() {
	}

	@Override
	public int getSqlType() {
		return Types.INTEGER;
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	@Override
	public JavaTypeDescriptor getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Integer.class );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
		return new JdbcLiteralFormatterNumericData( javaTypeDescriptor, Integer.class );
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				st.setInt( index, javaTypeDescriptor.unwrap( value, Integer.class, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setInt( name, javaTypeDescriptor.unwrap( value, Integer.class, options ) );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( rs.getInt( name ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getInt( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getInt( name ), options );
			}
		};
	}
}
