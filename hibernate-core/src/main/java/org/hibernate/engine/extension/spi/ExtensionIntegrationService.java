/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.extension.spi;

import org.hibernate.Incubating;
import org.hibernate.service.Service;

@Incubating
public interface ExtensionIntegrationService extends Service {
	/**
	 * Retrieve all extensions.
	 *
	 * @return All extensions.
	 */
	Iterable<ExtensionIntegration<?>> extensionIntegrations();
}
