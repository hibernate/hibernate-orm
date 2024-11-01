/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc;


import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;

/**
 * Convenient base class for proxy-based {@link LobCreator} for handling wrapping.
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
		return clob instanceof NClob nclob
				? wrap( nclob )
				: SerializableClobProxy.generateProxy( clob );
	}

	@Override
	public NClob wrap(NClob nclob) {
		return SerializableNClobProxy.generateProxy( nclob );
	}

	@Override
	public Blob createBlob(Blob blob) {
		return blob;
	}

	@Override
	public Clob createClob(Clob clob) {
		return clob;
	}

	@Override
	public NClob createNClob(NClob clob) {
		return clob;
	}
}
