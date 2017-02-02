/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;

/**
 * Convenient base class for proxy-based LobCreator for handling wrapping.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractLobCreator implements LobCreator {
	@Override
	public Blob wrap(Blob blob) {
		return SerializableBlobProxy.generateProxy( blob );
	}

	@Override
	public Clob wrap(Clob clob) {
		if ( NClob.class.isInstance( clob ) ) {
			return wrap( (NClob) clob );
		}
		else {
			return SerializableClobProxy.generateProxy( clob );
		}
	}

	@Override
	public NClob wrap(NClob nclob) {
		return SerializableNClobProxy.generateProxy( nclob );
	}
}
