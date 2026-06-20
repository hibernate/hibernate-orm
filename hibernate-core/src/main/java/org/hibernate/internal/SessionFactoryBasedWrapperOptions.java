/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;


import jakarta.annotation.Nonnull;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.format.FormatMapper;

/**
 * An incomplete implementation of {@link WrapperOptions} which is not backed by a session.
 *
 * @see SessionFactoryImplementor#getWrapperOptions()
 *
 * @author Christian Beikov
 */
class SessionFactoryBasedWrapperOptions implements WrapperOptions {

	private final SessionFactoryImplementor factory;

	SessionFactoryBasedWrapperOptions(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	@Override
	@Nonnull
	public SharedSessionContractImplementor getSession() {
		throw new UnsupportedOperationException( "No session" );
	}

	@Override
	@Nonnull
	public SessionFactoryImplementor getSessionFactory() {
		return factory;
	}

	@Override
	@Nonnull
	public LobCreator getLobCreator() {
		return factory.getJdbcServices().getLobCreator( getSession() );
	}

	@Override
	@Nonnull
	public FormatMapper getXmlFormatMapper() {
		return factory.getSessionFactoryOptions().getXmlFormatMapper();
	}

	@Override
	@Nonnull
	public FormatMapper getJsonFormatMapper() {
		return factory.getSessionFactoryOptions().getJsonFormatMapper();
	}
}
