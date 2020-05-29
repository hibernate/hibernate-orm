/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	protected static final DefaultSessionFactoryBuilderService INSTANCE = new DefaultSessionFactoryBuilderService();

	private DefaultSessionFactoryBuilderService() {
	}

	@Override
	public SessionFactoryBuilderImplementor createSessionFactoryBuilder(final MetadataImpl metadata, final BootstrapContext bootstrapContext) {
		return new SessionFactoryBuilderImpl( metadata, bootstrapContext );
	}

}
