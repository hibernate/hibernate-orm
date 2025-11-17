/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.jdbc;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;

public class SharedDriverManagerTypeCacheClearingIntegrator implements Integrator {
	@Override
	public void integrate(
			Metadata metadata,
			BootstrapContext bootstrapContext,
			SessionFactoryImplementor sessionFactory) {
		SharedDriverManagerConnectionProvider.getInstance().clearTypeCache();
	}
}
