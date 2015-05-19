/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
