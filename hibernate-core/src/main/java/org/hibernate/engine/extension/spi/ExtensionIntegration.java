/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.extension.spi;

import org.hibernate.Incubating;
import org.hibernate.service.JavaServiceLoadable;

@Incubating
@JavaServiceLoadable
public interface ExtensionIntegration<E extends Extension> {
	/**
	 * The extension contract created by this integration.
	 */
	Class<E> getExtensionType();

	/**
	 * Create an extension for a newly opened session.
	 */
	E createExtension(ExtensionIntegrationContext context);
}
