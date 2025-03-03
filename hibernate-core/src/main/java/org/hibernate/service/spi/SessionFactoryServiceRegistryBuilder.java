/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.spi;

import org.hibernate.service.Service;

/**
 * @author Steve Ebersole
 */
public interface SessionFactoryServiceRegistryBuilder {
	SessionFactoryServiceRegistryBuilder addInitiator(SessionFactoryServiceInitiator<?> initiator);

	<R extends Service> SessionFactoryServiceRegistryBuilder addService(Class<R> serviceRole, R service);
}
