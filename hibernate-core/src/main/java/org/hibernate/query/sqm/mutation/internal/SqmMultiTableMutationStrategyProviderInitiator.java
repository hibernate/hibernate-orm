/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.mutation.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategyProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Initiator for SqmMultiTableMutationStrategyProvider service
 *
 * @author Steve Ebersole
 */
public class SqmMultiTableMutationStrategyProviderInitiator implements StandardServiceInitiator<SqmMultiTableMutationStrategyProvider> {
	/**
	 * Singleton access
	 */
	public static final SqmMultiTableMutationStrategyProviderInitiator INSTANCE = new SqmMultiTableMutationStrategyProviderInitiator();

	@Override
	public SqmMultiTableMutationStrategyProvider initiateService(
			Map<String, Object> configurationValues,
			ServiceRegistryImplementor registry) {
		return new SqmMultiTableMutationStrategyProviderStandard();
	}

	@Override
	public Class<SqmMultiTableMutationStrategyProvider> getServiceInitiated() {
		return SqmMultiTableMutationStrategyProvider.class;
	}
}
