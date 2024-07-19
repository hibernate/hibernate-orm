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
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformResolver;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.jboss.logging.Logger;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Standard initiator for the standard {@link JtaPlatform}
 *
 * @author Steve Ebersole
 */
public class JtaPlatformInitiator implements StandardServiceInitiator<JtaPlatform> {
	public static final JtaPlatformInitiator INSTANCE = new JtaPlatformInitiator();

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, JtaPlatformInitiator.class.getName());

	@Override
	public Class<JtaPlatform> getServiceInitiated() {
		return JtaPlatform.class;
	}

	@Override
	public @Nullable JtaPlatform initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		final Object setting = configurationValues.get( AvailableSettings.JTA_PLATFORM );
		JtaPlatform platform = registry.requireService( StrategySelector.class )
				.resolveStrategy( JtaPlatform.class, setting );

		if ( platform == null ) {
			LOG.debug( "No JtaPlatform was specified, checking resolver" );
			platform = registry.requireService( JtaPlatformResolver.class )
					.resolveJtaPlatform( configurationValues, registry );
		}

		if ( platform == null ) {
			LOG.debug( "No JtaPlatform was specified, checking fallback provider" );
			platform = getFallbackProvider( configurationValues, registry );
		}

		if ( platform != null && !(platform instanceof NoJtaPlatform) ) {
			LOG.usingJtaPlatform( platform.getClass().getName() );
		}
		else {
			LOG.noJtaPlatform();
		}
		return platform;
	}

	protected @Nullable JtaPlatform getFallbackProvider(Map<?,?> configurationValues, ServiceRegistryImplementor registry) {
		return null;
	}
}
