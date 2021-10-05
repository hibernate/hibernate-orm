/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.ByteJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.TinyIntJdbcTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#TINYINT TINYINT} and {@link Byte}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class ByteType
		extends AbstractSingleColumnStandardBasicType<Byte> {

	public static final ByteType INSTANCE = new ByteType();

	public ByteType() {
		super( TinyIntJdbcTypeDescriptor.INSTANCE, ByteJavaTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "byte";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] {getName(), byte.class.getName(), Byte.class.getName()};
	}
}
