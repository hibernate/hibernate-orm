/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;

/**
 * @author Christian Beikov
 */
public class NamedBasicTypeImpl<J> extends BasicTypeImpl<J> {

	private final String name;

	public NamedBasicTypeImpl(JavaTypeDescriptor<J> jtd, JdbcTypeDescriptor std, String name) {
		super( jtd, std );
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

}
