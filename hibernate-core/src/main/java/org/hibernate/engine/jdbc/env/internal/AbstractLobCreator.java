/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.internal;


import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.proxy.WrappedBlob;
import org.hibernate.engine.jdbc.proxy.WrappedClob;
import org.hibernate.engine.jdbc.proxy.WrappedNClob;
import org.hibernate.engine.jdbc.proxy.SerializableBlobProxy;
import org.hibernate.engine.jdbc.proxy.SerializableClobProxy;
import org.hibernate.engine.jdbc.proxy.SerializableNClobProxy;

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
	public Blob toJdbcBlob(Blob blob) {
		if ( blob instanceof WrappedBlob wrappedBlob ) {
			blob = wrappedBlob.getWrappedBlob();
		}
		return blob;
	}

	@Override
	public Clob toJdbcClob(Clob clob) {
		if ( clob instanceof WrappedClob wrappedClob ) {
			clob = wrappedClob.getWrappedClob();
		}
		return clob;
	}

	@Override
	public NClob toJdbcNClob(NClob clob) {
		if ( clob instanceof WrappedNClob wrappedNClob ) {
			clob = wrappedNClob.getWrappedNClob();
		}
		return clob;
	}
}
