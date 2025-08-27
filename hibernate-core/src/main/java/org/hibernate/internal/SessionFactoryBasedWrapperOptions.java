/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import java.util.TimeZone;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.spi.TypeConfiguration;

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
	public SharedSessionContractImplementor getSession() {
		throw new UnsupportedOperationException( "No session" );
	}

	@Override
	public boolean useStreamForLobBinding() {
		return factory.getJdbcServices().getJdbcEnvironment().getDialect().useInputStreamToInsertBlob();
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return factory.getSessionFactoryOptions().getPreferredSqlTypeCodeForBoolean();
	}

	@Override
	public LobCreator getLobCreator() {
		return factory.getJdbcServices().getLobCreator( getSession() );
	}

	@Override
	public TimeZone getJdbcTimeZone() {
		return factory.getSessionFactoryOptions().getJdbcTimeZone();
	}

	@Override
	public Dialect getDialect() {
		return factory.getJdbcServices().getJdbcEnvironment().getDialect();
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return factory.getTypeConfiguration();
	}

	@Override
	public FormatMapper getXmlFormatMapper() {
		return factory.getSessionFactoryOptions().getXmlFormatMapper();
	}

	@Override
	public FormatMapper getJsonFormatMapper() {
		return factory.getSessionFactoryOptions().getJsonFormatMapper();
	}
}
