package org.hibernate.engine.extension.spi;

import org.hibernate.Incubating;

@Incubating(since = "7.4")
public interface ExtensionIntegrationService {
	/**
	 * Retrieve all Java-service-loaded extension integrations.
	 *
	 * @return All extension integrations.
	 */
	Iterable<ExtensionIntegration<?>> extensionIntegrations();
}
