/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
