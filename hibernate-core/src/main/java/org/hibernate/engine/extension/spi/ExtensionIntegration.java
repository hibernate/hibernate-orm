/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.extension.spi;

import org.hibernate.Incubating;

@Incubating
public interface ExtensionIntegration<E extends Extension> {
	Class<E> getExtensionType();

	E createExtension(ExtensionIntegrationContext context);
}
