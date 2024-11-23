/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.boot;

import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfoSource;

import org.hibernate.testing.env.ConnectionProviderBuilder;

/**
 * @author Steve Ebersole
 */
public class DialectFactoryTestingImpl implements DialectFactory {
	private final Dialect dialect;

	public DialectFactoryTestingImpl() {
		this( ConnectionProviderBuilder.getCorrespondingDialect() );
	}

	public DialectFactoryTestingImpl(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public Dialect buildDialect(Map<String,Object> configValues, DialectResolutionInfoSource resolutionInfoSource) {
		return dialect;
	}
}
