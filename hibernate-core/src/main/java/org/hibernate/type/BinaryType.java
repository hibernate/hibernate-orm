/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.Types;

import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcTypeDescriptor;

/**
 * A type that maps between a {@link Types#VARBINARY VARBINARY} and {@code byte[]}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class BinaryType
		extends AbstractSingleColumnStandardBasicType<byte[]>
		implements AdjustableBasicType<byte[]> {

	public static final BinaryType INSTANCE = new BinaryType();

	public String getName() {
		return "binary";
	}

	public BinaryType() {
		super( VarbinaryJdbcTypeDescriptor.INSTANCE, PrimitiveByteArrayJavaTypeDescriptor.INSTANCE );
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), "byte[]", byte[].class.getName() };
	}

}
