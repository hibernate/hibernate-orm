/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.NClob;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.NClobTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#NCLOB NCLOB} and {@link java.sql.NClob}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class NClobType extends AbstractSingleColumnStandardBasicType<NClob> {
	public static final NClobType INSTANCE = new NClobType();

	public NClobType() {
		super( org.hibernate.type.descriptor.sql.NClobTypeDescriptor.DEFAULT, NClobTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "nclob";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	protected NClob getReplacement(NClob original, NClob target, SharedSessionContractImplementor session) {
		if ( target == null ) {
			return copyOriginalNClob( original, session );
		}
		return session.getJdbcServices().getJdbcEnvironment().getDialect().getLobMergeStrategy().mergeNClob( original, target, session );
	}

	private NClob copyOriginalNClob(NClob original, SharedSessionContractImplementor session) {
		try {
			final LobCreator lobCreator = session.getFactory().getServiceRegistry().getService( JdbcServices.class ).getLobCreator( session );
			return original == null
					? lobCreator.createNClob( "" )
					: lobCreator.createNClob( original.getCharacterStream(), original.length() );
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert( e, "unable to merge NCLOB data" );
		}
	}
}
