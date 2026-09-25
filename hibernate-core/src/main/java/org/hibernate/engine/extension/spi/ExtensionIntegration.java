package org.hibernate.engine.extension.spi;

import org.hibernate.Incubating;

@Incubating(since = "7.4")
public interface ExtensionIntegration<E extends Extension> {
	Class<E> getExtensionType();

	E createExtension(ExtensionIntegrationContext context);
}
