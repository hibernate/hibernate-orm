/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.extension.spi;

import org.hibernate.Incubating;

@Incubating
public interface ExtensionIntegrationService {
	/**
	 * Retrieve all Java-service-loaded extension integrations.
	 *
	 * @return All extension integrations.
	 */
	Iterable<ExtensionIntegration<?>> extensionIntegrations();
}
