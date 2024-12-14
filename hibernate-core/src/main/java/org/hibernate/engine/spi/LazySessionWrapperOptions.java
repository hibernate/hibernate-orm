/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.util.TimeZone;

import org.hibernate.Internal;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.internal.FastSessionServices;
import org.hibernate.type.descriptor.WrapperOptions;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An implementation of {@link WrapperOptions} used for rendering SQL literals,
 * which is backed by the {@link SessionFactoryImplementor}, and which
 * {@linkplain SessionFactoryImplementor#openTemporarySession lazily creates a
 * temporary session if needed.} The temporary session will only be created when
 * dealing with LOBs.
 * <p>
 * This object is {@link AutoCloseable}, and <em>must</em> be explicitly cleaned
 * up by its creator.
 *
 * @apiNote This thing is nasty, and we should find a better way to solve the problem.
 * Whenever possible, just use {@link SessionFactoryImplementor#getWrapperOptions()}
 * instead.
 */
@Internal
public class LazySessionWrapperOptions implements WrapperOptions, AutoCloseable {

	private final SessionFactoryImplementor sessionFactory;
	private final FastSessionServices fastSessionServices;
	private @Nullable SessionImplementor session;

	public LazySessionWrapperOptions(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
		fastSessionServices = sessionFactory.getFastSessionServices();
	}

	public void cleanup() {
		if ( session != null ) {
			session.close();
			session = null;
		}
	}

	@Override
	public void close() {
		cleanup();
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		session = sessionFactory.openTemporarySession();
		return session;
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public boolean useStreamForLobBinding() {
		return fastSessionServices.useStreamForLobBinding;
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return fastSessionServices.preferredSqlTypeCodeForBoolean;
	}

	@Override
	public TimeZone getJdbcTimeZone() {
		return fastSessionServices.jdbcTimeZone;
	}

	@Override
	public Dialect getDialect() {
		return fastSessionServices.dialect;
	}

	@Override
	public LobCreator getLobCreator() {
		return fastSessionServices.jdbcServices.getLobCreator( getSession() );
	}
}
