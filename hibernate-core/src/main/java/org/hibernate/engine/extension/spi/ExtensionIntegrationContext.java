package org.hibernate.engine.extension.spi;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

@Incubating(since = "7.4")
public interface ExtensionIntegrationContext {

	SharedSessionContractImplementor getSession();
}
