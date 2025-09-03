/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import java.lang.invoke.MethodHandles;
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

	private static final CoreMessageLogger log = Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, JtaPlatformInitiator.class.getName() );

	@Override
	public Class<JtaPlatform> getServiceInitiated() {
		return JtaPlatform.class;
	}

	@Override
	public @Nullable JtaPlatform initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		final Object setting = configurationValues.get( AvailableSettings.JTA_PLATFORM );
		JtaPlatform platform =
				registry.requireService( StrategySelector.class )
						.resolveStrategy( JtaPlatform.class, setting );

		if ( platform == null ) {
			log.trace( "No JtaPlatform was specified, checking resolver" );
			platform = registry.requireService( JtaPlatformResolver.class )
					.resolveJtaPlatform( configurationValues, registry );
		}

		if ( platform == null ) {
			log.trace( "No JtaPlatform was specified, checking fallback provider" );
			platform = getFallbackProvider( configurationValues, registry );
		}

		if ( platform == null || platform instanceof NoJtaPlatform ) {
			log.noJtaPlatform();
		}
		else {
			log.usingJtaPlatform( platform.getClass().getName() );
		}
		return platform;
	}

	protected @Nullable JtaPlatform getFallbackProvider(Map<?,?> configurationValues, ServiceRegistryImplementor registry) {
		return null;
	}
}
