/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderService;

/**
 * This is the default implementation of SessionFactoryBuilderService, which just
 * returns the default SessionFactoryBuilderImpl.
 */
public final class DefaultSessionFactoryBuilderService implements SessionFactoryBuilderService {

	static final DefaultSessionFactoryBuilderService INSTANCE = new DefaultSessionFactoryBuilderService();

	private DefaultSessionFactoryBuilderService() {
	}

	@Override
	public SessionFactoryBuilderImplementor createSessionFactoryBuilder(final MetadataImpl metadata, final BootstrapContext bootstrapContext) {
		return new SessionFactoryBuilderImpl( metadata, bootstrapContext );
	}

}
