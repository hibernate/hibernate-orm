/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.results.jdbc.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducerProvider;

/**
 * Initiator for {@link JdbcValuesMappingProducerProviderStandard}
 *
 * @author Steve Ebersole
 */
public class JdbcValuesMappingProducerProviderInitiator
		implements StandardServiceInitiator<JdbcValuesMappingProducerProvider> {
	/**
	 * Singleton access
	 */
	public static final JdbcValuesMappingProducerProviderInitiator INSTANCE = new JdbcValuesMappingProducerProviderInitiator();

	@Override
	public JdbcValuesMappingProducerProvider initiateService(
			Map<String, Object> configurationValues,
			ServiceRegistryImplementor registry) {
		return JdbcValuesMappingProducerProviderStandard.INSTANCE;
	}

	@Override
	public Class<JdbcValuesMappingProducerProvider> getServiceInitiated() {
		return JdbcValuesMappingProducerProvider.class;
	}
}
