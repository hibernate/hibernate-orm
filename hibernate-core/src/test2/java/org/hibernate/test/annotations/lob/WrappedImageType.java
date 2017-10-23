/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.lob;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.ByteArrayTypeDescriptor;
import org.hibernate.type.descriptor.sql.LongVarbinaryTypeDescriptor;

/**
 * A type that maps JDBC {@link java.sql.Types#LONGVARBINARY LONGVARBINARY} and {@code Byte[]}
 * 
 * @author Strong Liu
 */
public class WrappedImageType extends AbstractSingleColumnStandardBasicType<Byte[]> {
	public static final WrappedImageType INSTANCE = new WrappedImageType();

	public WrappedImageType() {
		super( LongVarbinaryTypeDescriptor.INSTANCE, ByteArrayTypeDescriptor.INSTANCE );
	}

	public String getName() {
		// todo name these annotation types for addition to the registry
		return null;
	}
}
