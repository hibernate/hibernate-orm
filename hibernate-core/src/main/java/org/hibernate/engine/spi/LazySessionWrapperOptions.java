/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.util.TimeZone;

import org.hibernate.Internal;
import org.hibernate.type.descriptor.WrapperOptions;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A lazy session implementation that is needed for rendering literals.
 * Usually, only the {@link WrapperOptions} interface is needed,
 * but for creating LOBs, it might be to have a full-blown session.
 */
@Internal
public class LazySessionWrapperOptions extends AbstractDelegatingWrapperOptions {

	private final SessionFactoryImplementor sessionFactory;
	private @Nullable SessionImplementor session;

	public LazySessionWrapperOptions(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void cleanup() {
		if ( session != null ) {
			session.close();
			session = null;
		}
	}

	@Override
	protected SessionImplementor delegate() {
		if ( session == null ) {
			session = sessionFactory.openTemporarySession();
		}
		return session;
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		return delegate();
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public boolean useStreamForLobBinding() {
		return sessionFactory.getFastSessionServices().useStreamForLobBinding();
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return sessionFactory.getFastSessionServices().getPreferredSqlTypeCodeForBoolean();
	}

	@Override
	public TimeZone getJdbcTimeZone() {
		return sessionFactory.getSessionFactoryOptions().getJdbcTimeZone();
	}
}
