/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.sql;

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
 * Descriptor for
 * @author Steve Ebersole
 */
public class ObjectSqlTypeDescriptor implements SqlTypeDescriptor {
	/**
	 * Singleton access
	 */
	public static final ObjectSqlTypeDescriptor INSTANCE = new ObjectSqlTypeDescriptor( Types.JAVA_OBJECT );

	private final int jdbcTypeCode;

	public ObjectSqlTypeDescriptor(int jdbcTypeCode) {
		this.jdbcTypeCode = jdbcTypeCode;
	}

	@Override
	public int getSqlType() {
		return jdbcTypeCode;
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
		if ( Serializable.class.isAssignableFrom( javaTypeDescriptor.getJavaType() ) ) {
			return VarbinaryTypeDescriptor.INSTANCE.getBinder( javaTypeDescriptor );
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
	public ValueExtractor getExtractor(JavaTypeDescriptor javaTypeDescriptor) {
		if ( Serializable.class.isAssignableFrom( javaTypeDescriptor.getJavaType() ) ) {
			return VarbinaryTypeDescriptor.INSTANCE.getExtractor( javaTypeDescriptor );
		}

		return new BasicExtractor( javaTypeDescriptor, this ) {
			@Override
			protected Object doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return rs.getObject( paramIndex );
			}

			@Override
			protected Object doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return statement.getObject( index );
			}

			@Override
			protected Object doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return statement.getObject( name );
			}
		};
	}
}
