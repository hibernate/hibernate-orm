/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.Blob;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.BlobTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#BLOB BLOB} and {@link Blob}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class BlobType extends AbstractSingleColumnStandardBasicType<Blob> {
	public static final BlobType INSTANCE = new BlobType();

	public BlobType() {
		super( org.hibernate.type.descriptor.sql.BlobTypeDescriptor.DEFAULT, BlobTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "blob";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	protected Blob getReplacement(Blob original, Blob target, SharedSessionContractImplementor session) {
		return session.getJdbcServices().getJdbcEnvironment().getDialect().getLobMergeStrategy().mergeBlob( original, target, session );
	}

}
