/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jmx.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jmx.spi.JmxService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Standard initiator for the standard {@link JmxService} service
 *
 * @author Steve Ebersole
 */
public class JmxServiceInitiator implements StandardServiceInitiator<JmxService> {
	public static final JmxServiceInitiator INSTANCE = new JmxServiceInitiator();

	@Override
	public Class<JmxService> getServiceInitiated() {
		return JmxService.class;
	}

	@Override
	public JmxService initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return ConfigurationHelper.getBoolean( AvailableSettings.JMX_ENABLED, configurationValues, false )
				? new JmxServiceImpl( configurationValues )
				: DisabledJmxServiceImpl.INSTANCE;
	}
}
