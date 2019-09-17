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

/**
 * Standard initiator for the standard {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform}
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
	@SuppressWarnings( {"unchecked"})
	public JtaPlatform initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final Object setting = configurationValues.get( AvailableSettings.JTA_PLATFORM );
		JtaPlatform platform = registry.getService( StrategySelector.class ).resolveStrategy( JtaPlatform.class, setting );

		if ( platform == null ) {
			LOG.debug( "No JtaPlatform was specified, checking resolver" );
			platform = registry.getService( JtaPlatformResolver.class ).resolveJtaPlatform( configurationValues, registry );
		}

		if ( platform == null ) {
			LOG.debug( "No JtaPlatform was specified, checking resolver" );
			platform = getFallbackProvider( configurationValues, registry );
		}

		LOG.usingJtaPlatform( platform != null ? platform.getClass().getName() : "null" );
		return platform;
	}

	@SuppressWarnings({"WeakerAccess", "unused"})
	protected JtaPlatform getFallbackProvider(Map configurationValues, ServiceRegistryImplementor registry) {
		return null;
	}
}
