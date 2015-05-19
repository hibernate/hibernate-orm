/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayTypeDescriptor;
import org.hibernate.type.descriptor.sql.BlobTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#BLOB BLOB} and {@code byte[]}
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class MaterializedBlobType extends AbstractSingleColumnStandardBasicType<byte[]> {
	public static final MaterializedBlobType INSTANCE = new MaterializedBlobType();

	public MaterializedBlobType() {
		super( BlobTypeDescriptor.DEFAULT, PrimitiveByteArrayTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "materialized_blob";
	}
}
