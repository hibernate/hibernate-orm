/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;
import org.hibernate.type.descriptor.java.ByteArrayTypeDescriptor;
import org.hibernate.type.descriptor.sql.BlobTypeDescriptor;

/**
 * A type that maps JDBC {@link java.sql.Types#BLOB BLOB} and {@code Byte[]}.
 * A type that maps an SQL BLOB to Java Byte[].
 *
 * @author Strong Liu
 */
public class WrappedMaterializedBlobType extends AbstractSingleColumnStandardBasicType<Byte[]> {
	public static final WrappedMaterializedBlobType INSTANCE = new WrappedMaterializedBlobType();

	public WrappedMaterializedBlobType() {
		super( BlobTypeDescriptor.DEFAULT, ByteArrayTypeDescriptor.INSTANCE );
	}

	public String getName() {
		// todo name these annotation types for addition to the registry
		return null;
	}
}
