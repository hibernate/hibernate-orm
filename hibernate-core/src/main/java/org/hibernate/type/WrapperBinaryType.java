/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.Types;

import org.hibernate.type.descriptor.java.ByteArrayJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcTypeDescriptor;

/**
 * A type mapping {@link Types#VARBINARY VARBINARY} and {@link Byte Byte[]}
 * 
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class WrapperBinaryType extends AbstractSingleColumnStandardBasicType<Byte[]> implements AdjustableBasicType<Byte[]> {
	public static final WrapperBinaryType INSTANCE = new WrapperBinaryType();

	public WrapperBinaryType() {
		super( VarbinaryJdbcTypeDescriptor.INSTANCE, ByteArrayJavaTypeDescriptor.INSTANCE );
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), "Byte[]", Byte[].class.getName() };
	}

	public String getName() {
		//TODO find a decent name before documenting
		return "wrapper-binary";
	}
}
