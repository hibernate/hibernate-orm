/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.internal;

import java.util.Map;

import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Stoppable;

import org.jboss.logging.Logger;

/**
 * Provides central access to Envers' configuration.
 *
 * In many ways, this replaces the legacy static map Envers used originally as
 * a means to share the old AuditConfiguration.
 *
 * @author Steve Ebersole
 * @author Chris Cranford
 */
public class EnversServiceImpl implements EnversService, Configurable, Stoppable {
	private static final Logger log = Logger.getLogger( EnversServiceImpl.class );

	private static final String INTEGRATION_ENABLED = "hibernate.integration.envers.enabled";
	private static final String LEGACY_AUTO_REGISTER = "hibernate.listeners.envers.autoRegister";

	private boolean integrationEnabled;

	@Override
	public void configure(Map configurationValues) {
		if ( configurationValues.containsKey( LEGACY_AUTO_REGISTER ) ) {
			log.debugf(
					"Encountered deprecated Envers setting [%s]; use [%s] or [%s] instead",
					LEGACY_AUTO_REGISTER,
					INTEGRATION_ENABLED,
					EnversIntegrator.AUTO_REGISTER
			);
		}

		this.integrationEnabled = ConfigurationHelper.getBoolean(
				INTEGRATION_ENABLED,
				configurationValues,
				true
		);
		log.infof( "Envers integration enabled? : %s", integrationEnabled );
	}

	@Override
	public boolean isEnabled() {
		return integrationEnabled;
	}

	@Override
	public void stop() {
		// anything to release?
	}

}
