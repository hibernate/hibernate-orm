/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.UUIDJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeDescriptorRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Specialized type mapping for {@link UUID} and the Postgres UUID data type (which is mapped as OTHER in its
 * JDBC driver).
 *
 * @author Steve Ebersole
 * @author David Driscoll
 */
public class PostgresUUIDType extends AbstractSingleColumnStandardBasicType<UUID> {
	public static final PostgresUUIDType INSTANCE = new PostgresUUIDType();

	public PostgresUUIDType() {
		super( PostgresUUIDJdbcType.INSTANCE, UUIDJavaTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "pg-uuid";
	}

	@Override
	protected boolean registerUnderJavaType() {
		// register this type under UUID when it is added to the basic type registry
		return true;
	}

	public static class PostgresUUIDJdbcType implements JdbcType {
		/**
		 * Singleton access
		 */
		public static final PostgresUUIDJdbcType INSTANCE = new PostgresUUIDJdbcType();

		/**
		 * Postgres reports its UUID type as {@link Types#OTHER}.  Unfortunately
		 * it reports a lot of its types as {@link Types#OTHER}, making that
		 * value useless for distinguishing one SqlTypeDescriptor from another.
		 * So here we define a "magic value" that is a (hopefully no collisions)
		 * unique key within the {@link JdbcTypeDescriptorRegistry}
		 */
		private static final int JDBC_TYPE_CODE = 3975;

		@Override
		public int getJdbcTypeCode() {
			return JDBC_TYPE_CODE;
		}

		@Override
		public <J> BasicJavaType<J> getJdbcRecommendedJavaTypeMapping(
				Integer length,
				Integer scale,
				TypeConfiguration typeConfiguration) {
			return (BasicJavaType<J>) typeConfiguration.getJavaTypeDescriptorRegistry().resolveDescriptor( UUID.class );
		}

		@Override
		public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaTypeDescriptor) {
			return null;
		}

		@Override
		public <X> ValueBinder<X> getBinder(JavaType<X> javaTypeDescriptor) {
			return new BasicBinder<X>( javaTypeDescriptor, this ) {

				@Override
				protected void doBindNull(PreparedStatement st, int index, WrapperOptions wrapperOptions) throws SQLException {
					st.setNull( index, Types.OTHER );
				}
				@Override
				protected void doBind(
						PreparedStatement st,
						X value,
						int index,
						WrapperOptions wrapperOptions) throws SQLException {
					st.setObject( index, javaTypeDescriptor.unwrap( value, UUID.class, wrapperOptions ), Types.OTHER );
				}

				@Override
				protected void doBindNull(CallableStatement st, String name, WrapperOptions wrapperOptions) throws SQLException {
					st.setNull( name, Types.OTHER );
				}
				@Override
				protected void doBind(
						CallableStatement st,
						X value,
						String name,
						WrapperOptions wrapperOptions) throws SQLException {
					st.setObject( name, javaTypeDescriptor.unwrap( value, UUID.class, wrapperOptions ), Types.OTHER );
				}
			};
		}

		@Override
		public <X> ValueExtractor<X> getExtractor(JavaType<X> javaTypeDescriptor) {
			return new BasicExtractor<X>( javaTypeDescriptor, this ) {
				@Override
				protected X doExtract(ResultSet rs, int position, WrapperOptions wrapperOptions) throws SQLException {
					return javaTypeDescriptor.wrap( rs.getObject( position ), wrapperOptions );
				}

				@Override
				protected X doExtract(CallableStatement statement, int position, WrapperOptions wrapperOptions) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getObject( position ), wrapperOptions );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions wrapperOptions) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getObject( name ), wrapperOptions );
				}
			};
		}
	}
}
