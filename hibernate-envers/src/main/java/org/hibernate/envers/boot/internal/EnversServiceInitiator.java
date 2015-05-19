/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Steve Ebersole
 */
public class EnversServiceInitiator implements StandardServiceInitiator<EnversService> {
	/**
	 * Singleton access
	 */
	public static final EnversServiceInitiator INSTANCE = new EnversServiceInitiator();

	@Override
	public EnversService initiateService(
			Map configurationValues,
			ServiceRegistryImplementor registry) {
		return new EnversServiceImpl();
	}

	@Override
	public Class<EnversService> getServiceInitiated() {
		return EnversService.class;
	}
}
