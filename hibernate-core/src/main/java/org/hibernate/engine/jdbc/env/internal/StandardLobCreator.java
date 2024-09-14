/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.env.internal;

import java.io.Reader;
import java.sql.Connection;
import java.sql.NClob;
import java.sql.SQLException;

import org.hibernate.JDBCException;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.NonContextualLobCreator;

/**
 * {@linkplain LobCreator} implementation using {@linkplain Connection#createBlob},
 * {@linkplain Connection#createClob} and {@linkplain Connection#createNClob} to
 * create the LOB references.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class StandardLobCreator extends BlobAndClobCreator {
	/**
	 * Callback for performing contextual NCLOB creation
	 */
	public static final LobCreationContext.Callback<NClob> CREATE_NCLOB_CALLBACK = Connection::createNClob;

	public StandardLobCreator(LobCreationContext lobCreationContext) {
		super( lobCreationContext );
	}

	/**
	 * Create the basic contextual NCLOB reference.
	 *
	 * @return The created NCLOB reference.
	 */
	public NClob createNClob() {
		return lobCreationContext.fromContext( CREATE_NCLOB_CALLBACK );
	}

	@Override
	public NClob createNClob(String string) {
		try {
			final NClob nclob = createNClob();
			nclob.setString( 1, string );
			return nclob;
		}
		catch ( SQLException e ) {
			throw new JDBCException( "Unable to set NCLOB string after creation", e );
		}
	}

	@Override
	public NClob createNClob(Reader reader, long length) {
		// IMPL NOTE : it is inefficient to use JDBC LOB locator creation to create a LOB
		// backed by a given stream.  So just wrap the stream (which is what the NonContextualLobCreator does).
		return NonContextualLobCreator.INSTANCE.createNClob( reader, length );
	}
}
