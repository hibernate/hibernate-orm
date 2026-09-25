package org.hibernate.engine.extension.spi;

import org.hibernate.Incubating;
import org.hibernate.service.Service;

@Incubating(since = "7.4")
public interface ExtensionIntegrationService extends Service {
	/**
	 * Retrieve all extensions.
	 *
	 * @return All extensions.
	 */
	Iterable<ExtensionIntegration<?>> extensionIntegrations();
}
