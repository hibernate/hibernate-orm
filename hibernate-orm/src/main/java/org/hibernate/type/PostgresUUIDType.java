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
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.UUIDTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicBinder;
import org.hibernate.type.descriptor.sql.BasicExtractor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

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
		super( PostgresUUIDSqlTypeDescriptor.INSTANCE, UUIDTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "pg-uuid";
	}

	@Override
	protected boolean registerUnderJavaType() {
		// register this type under UUID when it is added to the basic type registry
		return true;
	}

	public static class PostgresUUIDSqlTypeDescriptor implements SqlTypeDescriptor {
		public static final PostgresUUIDSqlTypeDescriptor INSTANCE = new PostgresUUIDSqlTypeDescriptor();

		public int getSqlType() {
			// ugh
			return Types.OTHER;
		}

		@Override
		public boolean canBeRemapped() {
			return true;
		}

		public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
					st.setObject( index, javaTypeDescriptor.unwrap( value, UUID.class, options ), getSqlType() );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					st.setObject( name, javaTypeDescriptor.unwrap( value, UUID.class, options ), getSqlType() );
				}
			};
		}

		public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicExtractor<X>( javaTypeDescriptor, this ) {
				@Override
				protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
					return javaTypeDescriptor.wrap( rs.getObject( name ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getObject( index ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getObject( name ), options );
				}
			};
		}
	}
}
