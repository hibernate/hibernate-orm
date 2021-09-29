/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.JavaObjectTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.ObjectJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.ObjectNullResolvingJdbcTypeDescriptor;

/**
 * @author Christian Beikov
 */
public class NullType extends JavaObjectType {
	/**
	 * Singleton access
	 */
	public static final NullType INSTANCE = new NullType();

	public NullType() {
		super( ObjectNullResolvingJdbcTypeDescriptor.INSTANCE, JavaObjectTypeDescriptor.INSTANCE );
	}

	public NullType(JdbcTypeDescriptor jdbcTypeDescriptor, JavaTypeDescriptor<Object> javaTypeDescriptor) {
		super( jdbcTypeDescriptor, javaTypeDescriptor );
	}

	@Override
	public String getName() {
		return "null";
	}
}
