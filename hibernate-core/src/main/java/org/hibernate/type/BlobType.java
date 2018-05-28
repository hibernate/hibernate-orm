/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.Blob;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
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
		if ( target == null ) {
			return copyOriginalBlob( (Blob) original, session );
		}

		return session.getJdbcServices().getJdbcEnvironment().getDialect().getLobMergeStrategy().mergeBlob( original, target, session );
	}

	private Blob copyOriginalBlob(Blob original, SharedSessionContractImplementor session) {
		try {
			final LobCreator lobCreator = session.getFactory().getServiceRegistry().getService( JdbcServices.class ).getLobCreator(
					session );
			return original == null
					? lobCreator.createBlob( ArrayHelper.EMPTY_BYTE_ARRAY )
					: lobCreator.createBlob( original.getBinaryStream(), original.length() );
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert( e, "unable to merge BLOB data" );
		}
	}
}
