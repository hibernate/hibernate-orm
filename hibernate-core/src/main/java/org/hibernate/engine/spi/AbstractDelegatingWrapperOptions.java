/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.util.TimeZone;

import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * @author Christian Beikov
 * @author Andrea Boriero
 */
public abstract class AbstractDelegatingWrapperOptions implements WrapperOptions {

	/**
	 * Returns the underlying session delegate.
	 */
	protected abstract SessionImplementor delegate();

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return delegate().getSessionFactory();
	}

	@Override
	public boolean useStreamForLobBinding() {
		return delegate().useStreamForLobBinding();
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return delegate().getPreferredSqlTypeCodeForBoolean();
	}

	@Override
	public LobCreator getLobCreator() {
		return delegate().getLobCreator();
	}

	@Override
	public TimeZone getJdbcTimeZone() {
		return delegate().getJdbcTimeZone();
	}
}
