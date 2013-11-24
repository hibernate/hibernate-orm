/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformResolver;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class JtaPlatformResolverInitiator implements StandardServiceInitiator<JtaPlatformResolver> {
	public static final JtaPlatformResolverInitiator INSTANCE = new JtaPlatformResolverInitiator();

	private static final Logger log = Logger.getLogger( JtaPlatformResolverInitiator.class );

	@Override
	public JtaPlatformResolver initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final Object setting = configurationValues.get( AvailableSettings.JTA_PLATFORM_RESOLVER );
		final JtaPlatformResolver resolver = registry.getService( StrategySelector.class )
				.resolveStrategy( JtaPlatformResolver.class, setting );
		if ( resolver == null ) {
			log.debugf( "No JtaPlatformResolver was specified, using default [%s]", StandardJtaPlatformResolver.class.getName() );
			return StandardJtaPlatformResolver.INSTANCE;
		}
		return resolver;
	}

	@Override
	public Class<JtaPlatformResolver> getServiceInitiated() {
		return JtaPlatformResolver.class;
	}
}
