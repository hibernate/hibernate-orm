/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.TimeZone;

public class TypeConfigurationWrapperOptions implements WrapperOptions {

	private final TypeConfiguration typeConfiguration;

	public TypeConfigurationWrapperOptions(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
	}

	@Override
	public Dialect getDialect() {
		return typeConfiguration.getCurrentBaseSqlTypeIndicators().getDialect();
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		return typeConfiguration.getSessionFactory().getWrapperOptions().getSession();
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return typeConfiguration.getSessionFactory();
	}

	@Override
	public boolean useStreamForLobBinding() {
		return typeConfiguration.getSessionFactory().getWrapperOptions().useStreamForLobBinding();
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return typeConfiguration.getCurrentBaseSqlTypeIndicators().getPreferredSqlTypeCodeForBoolean();
	}

	@Override
	public LobCreator getLobCreator() {
		return typeConfiguration.getSessionFactory().getWrapperOptions().getLobCreator();
	}

	@Override
	public TimeZone getJdbcTimeZone() {
		return typeConfiguration.getSessionFactory().getWrapperOptions().getJdbcTimeZone();
	}
}
