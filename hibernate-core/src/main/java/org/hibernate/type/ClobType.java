/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.Clob;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.ClobTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#CLOB CLOB} and {@link Clob}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class ClobType extends AbstractSingleColumnStandardBasicType<Clob> {
	public static final ClobType INSTANCE = new ClobType();

	public ClobType() {
		super( org.hibernate.type.descriptor.sql.ClobTypeDescriptor.DEFAULT, ClobTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "clob";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	protected Clob getReplacement(Clob original, Clob target, SharedSessionContractImplementor session) {
		if ( target == null ) {
			return copyOriginalClob( (Clob) original, session );
		}
		return session.getJdbcServices().getJdbcEnvironment().getDialect().getLobMergeStrategy().mergeClob( (Clob) original, (Clob) target, session );
	}

	private Clob copyOriginalClob(Clob original, SharedSessionContractImplementor session) {
		try {
			final LobCreator lobCreator = session.getFactory().getServiceRegistry().getService( JdbcServices.class ).getLobCreator( session );
			return original == null
					? lobCreator.createClob( "" )
					: lobCreator.createClob( original.getCharacterStream(), original.length() );
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert( e, "unable to merge CLOB data" );
		}
	}
}
