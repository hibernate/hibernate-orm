/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.descriptor.java.internal.PrimitiveByteArrayJavaDescriptor;
import org.hibernate.type.spi.descriptor.sql.LongVarbinaryTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#LONGVARBINARY LONGVARBINARY} and {@code byte[]}
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class ImageType extends BasicTypeImpl<byte[]> {
	public static final ImageType INSTANCE = new ImageType();

	protected ImageType() {
		super( PrimitiveByteArrayJavaDescriptor.INSTANCE, LongVarbinaryTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "image";
	}
}
