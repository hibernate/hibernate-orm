/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.internal.BasicTypeImpl;

/**
 * A BasicType adapter targeting partial portability to 6.0's type
 * system changes.  In 6.0 the notion of a BasicType is just a
 * combination of JavaTypeDescriptor/SqlTypeDescriptor.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class StandardBasicTypeTemplate<J> extends BasicTypeImpl<J> {
	private final String name;
	private final String[] registrationKeys;

	public StandardBasicTypeTemplate(
			SqlTypeDescriptor sqlTypeDescriptor,
			BasicJavaDescriptor<J> javaTypeDescriptor,
			String... registrationKeys) {
		super( javaTypeDescriptor, sqlTypeDescriptor );
		this.registrationKeys = registrationKeys;

		this.name = javaTypeDescriptor.getJavaType() == null ? "(map-mode)" : javaTypeDescriptor.getJavaType().getName()
				+ " -> " + sqlTypeDescriptor.getSqlType();
	}

	public String getName() {
		return name;
	}

//	@Override
//	public String[] getRegistrationKeys() {
//		return registrationKeys;
//	}
}
