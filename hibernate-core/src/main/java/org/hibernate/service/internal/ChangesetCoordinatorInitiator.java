/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.internal;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.temporal.internal.ChangesetCoordinatorImpl;
import org.hibernate.temporal.spi.ChangesetCoordinator;

import java.util.Map;

/**
 * A service that acts as a source of transaction identifiers.
 *
 * @author Gavin King
 */
public class ChangesetCoordinatorInitiator implements StandardServiceInitiator<ChangesetCoordinator> {
	public static final ChangesetCoordinatorInitiator INSTANCE = new ChangesetCoordinatorInitiator();

	@Override
	public Class<ChangesetCoordinator> getServiceInitiated() {
		return ChangesetCoordinator.class;
	}

	@Override
	public ChangesetCoordinator initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		return new ChangesetCoordinatorImpl(registry);
	}
}
