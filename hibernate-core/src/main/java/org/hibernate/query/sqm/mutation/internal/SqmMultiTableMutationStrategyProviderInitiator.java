/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal;

import java.util.Map;

import jakarta.annotation.Nonnull;
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
			@Nonnull Map<String, Object> configurationValues,
			@Nonnull ServiceRegistryImplementor registry) {
		return new SqmMultiTableMutationStrategyProviderStandard();
	}

	@Nonnull
	@Override
	public Class<SqmMultiTableMutationStrategyProvider> getServiceInitiated() {
		return SqmMultiTableMutationStrategyProvider.class;
	}
}
