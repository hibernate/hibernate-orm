/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Descriptor for binding nulls with Types.NULL
 *
 * @author Christian Beikov
 */
public class NullJdbcTypeDescriptor implements JdbcTypeDescriptor {
	/**
	 * Singleton access
	 */
	public static final NullJdbcTypeDescriptor INSTANCE = new NullJdbcTypeDescriptor();

	@Override
	public int getJdbcTypeCode() {
		return Types.NULL;
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
		return null;
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicBinder<X>( javaTypeDescriptor, this ) {

			@Override
			protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
				st.setNull( index, Types.NULL );
			}

			@Override
			protected void doBindNull(CallableStatement st, String name, WrapperOptions options) throws SQLException {
				st.setNull( name, Types.NULL );
			}

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) {
				throw new UnsupportedOperationException( getClass().getName() + " should only be used to bind null!" );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) {
				throw new UnsupportedOperationException( getClass().getName() + " should only be used to bind null!" );
			}
		};
	}
}
