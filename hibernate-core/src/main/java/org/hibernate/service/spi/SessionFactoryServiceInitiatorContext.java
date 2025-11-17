/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.spi;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * @author Steve Ebersole
 */
public interface SessionFactoryServiceInitiatorContext {
	SessionFactoryImplementor getSessionFactory();
	SessionFactoryOptions getSessionFactoryOptions();
	ServiceRegistryImplementor getServiceRegistry();
}
