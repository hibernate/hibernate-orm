/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.jdbc;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Descriptor for binding objects
 *
 * @author Steve Ebersole
 */
public class ObjectJdbcTypeDescriptor implements JdbcTypeDescriptor {
	/**
	 * Singleton access
	 */
	public static final ObjectJdbcTypeDescriptor INSTANCE = new ObjectJdbcTypeDescriptor( Types.JAVA_OBJECT );

	private final int jdbcTypeCode;

	public ObjectJdbcTypeDescriptor(int jdbcTypeCode) {
		this.jdbcTypeCode = jdbcTypeCode;
	}

	@Override
	public int getJdbcTypeCode() {
		return jdbcTypeCode;
	}

	@Override
	public String toString() {
		return "ObjectSqlTypeDescriptor(" + jdbcTypeCode + ")";
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
		if ( Serializable.class.isAssignableFrom( javaTypeDescriptor.getJavaTypeClass() ) ) {
			return VarbinaryJdbcTypeDescriptor.INSTANCE.getBinder( javaTypeDescriptor );
		}

		return new BasicBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				st.setObject( index, value, jdbcTypeCode );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setObject( name, value, jdbcTypeCode );
			}
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> ValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
		if ( Serializable.class.isAssignableFrom( javaTypeDescriptor.getJavaTypeClass() ) ) {
			return VarbinaryJdbcTypeDescriptor.INSTANCE.getExtractor( javaTypeDescriptor );
		}

		return new BasicExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return (X) rs.getObject( paramIndex );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return (X) statement.getObject( index );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return (X) statement.getObject( name );
			}
		};
	}
}
