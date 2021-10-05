/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.LongVarbinaryJdbcTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#LONGVARBINARY LONGVARBINARY} and {@code byte[]}
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class ImageType extends AbstractSingleColumnStandardBasicType<byte[]> {
	public static final ImageType INSTANCE = new ImageType();

	public ImageType() {
		super( LongVarbinaryJdbcTypeDescriptor.INSTANCE, PrimitiveByteArrayJavaTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "image";
	}
}
