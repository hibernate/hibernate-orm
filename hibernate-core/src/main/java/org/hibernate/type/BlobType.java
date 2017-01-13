/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.Blob;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.descriptor.java.internal.BlobJavaDescriptor;

/**
 * A type that maps between {@link java.sql.Types#BLOB BLOB} and {@link Blob}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class BlobType extends BasicTypeImpl<Blob> {
	public static final BlobType INSTANCE = new BlobType();

	public BlobType() {
		super( BlobJavaDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.BlobTypeDescriptor.DEFAULT );
	}

	@Override
	public String getName() {
		return "blob";
	}

	@Override
	public Blob getReplacement(Blob original, Blob target, SharedSessionContractImplementor session) {
		return session.getJdbcServices().getJdbcEnvironment().getDialect().getLobMergeStrategy().mergeBlob( original, target, session );
	}

	@Override
	public JdbcLiteralFormatter<Blob> getJdbcLiteralFormatter() {
		// no literal support for BLOB data
		return null;
	}

}
