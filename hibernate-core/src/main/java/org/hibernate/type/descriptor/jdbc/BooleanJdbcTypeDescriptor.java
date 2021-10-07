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

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterBoolean;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#BOOLEAN BOOLEAN} handling.
 *
 * @author Steve Ebersole
 */
public class BooleanJdbcTypeDescriptor implements AdjustableJdbcTypeDescriptor {
	public static final BooleanJdbcTypeDescriptor INSTANCE = new BooleanJdbcTypeDescriptor();

	public BooleanJdbcTypeDescriptor() {
	}

	public int getJdbcTypeCode() {
		return Types.BOOLEAN;
	}

	@Override
	public String getFriendlyName() {
		return "BOOLEAN";
	}

	@Override
	public String toString() {
		return "BooleanTypeDescriptor";
	}

	@Override
	public <T> BasicJavaTypeDescriptor<T> getJdbcRecommendedJavaTypeMapping(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return (BasicJavaTypeDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Boolean.class );
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
		//noinspection unchecked
		return new JdbcLiteralFormatterBoolean( javaTypeDescriptor );
	}

	@Override
	public JdbcTypeDescriptor resolveIndicatedType(JdbcTypeDescriptorIndicators indicators, JavaTypeDescriptor<?> domainJtd) {
		final int preferredSqlTypeCodeForBoolean = indicators.getPreferredSqlTypeCodeForBoolean();
		// We treat BIT like BOOLEAN because it uses the same JDBC access methods
		if ( preferredSqlTypeCodeForBoolean != Types.BIT && preferredSqlTypeCodeForBoolean != Types.BOOLEAN ) {
			return indicators.getTypeConfiguration()
					.getJdbcTypeDescriptorRegistry()
					.getDescriptor( preferredSqlTypeCodeForBoolean );
		}
		return this;
	}

	public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
				st.setNull( index, options.getPreferredSqlTypeCodeForBoolean() );
			}

			@Override
			protected void doBindNull(CallableStatement st, String name, WrapperOptions options) throws SQLException {
				st.setNull( name, options.getPreferredSqlTypeCodeForBoolean() );;
			}

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				st.setBoolean( index, javaTypeDescriptor.unwrap( value, Boolean.class, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setBoolean( name, javaTypeDescriptor.unwrap( value, Boolean.class, options ) );
			}
		};
	}

	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( rs.getBoolean( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getBoolean( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getBoolean( name ), options );
			}
		};
	}
}
