/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Comparator;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.spi.VersionSupport;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.descriptor.java.internal.PrimitiveByteArrayJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.VarbinarySqlDescriptor;

/**
 * A type that maps between a {@link java.sql.Types#VARBINARY VARBINARY} and {@code byte[]}
 * specifically for entity versions/timestamps.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class RowVersionType
		extends BasicTypeImpl<byte[]>
		implements VersionSupport<byte[]> {

	public static final RowVersionType INSTANCE = new RowVersionType();

	public String getName() {
		return "row_version";
	}

	public RowVersionType() {
		super( PrimitiveByteArrayJavaDescriptor.INSTANCE, VarbinarySqlDescriptor.INSTANCE );
	}

	@Override
	public byte[] seed(SharedSessionContractImplementor session) {
		// Note : simply returns null for seed() and next() as the only known
		// 		application of binary types for versioning is for use with the
		// 		TIMESTAMP datatype supported by Sybase and SQL Server, which
		// 		are completely db-generated values...
		return null;
	}

	@Override
	public byte[] next(byte[] current, SharedSessionContractImplementor session) {
		return current;
	}

	@Override
	public Comparator<byte[]> getComparator() {
		return getJavaTypeDescriptor().getComparator();
	}
}
