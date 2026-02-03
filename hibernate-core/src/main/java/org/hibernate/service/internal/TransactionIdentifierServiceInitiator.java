/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.internal;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.TransactionIdentifierService;

import java.util.Map;

/**
 * A service that acts as a source of transaction identifiers.
 *
 * @author Gavin King
 */
public class TransactionIdentifierServiceInitiator implements StandardServiceInitiator<TransactionIdentifierService> {
	public static final TransactionIdentifierServiceInitiator INSTANCE = new TransactionIdentifierServiceInitiator();

	@Override
	public Class<TransactionIdentifierService> getServiceInitiated() {
		return TransactionIdentifierService.class;
	}

	@Override
	public TransactionIdentifierService initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		return new TransactionIdentifierServiceImpl(registry);
	}
}
