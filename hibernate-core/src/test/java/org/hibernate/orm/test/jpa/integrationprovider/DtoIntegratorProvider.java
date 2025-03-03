/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.integrationprovider;

import java.util.Collections;
import java.util.List;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;

/**
 * @author Andrea Boriero
 */
public class DtoIntegratorProvider implements IntegratorProvider {
	@Override
	public List<Integrator> getIntegrators() {
		return Collections.singletonList(
				new Integrator() {
					@Override
					public void integrate(
							Metadata metadata,
							BootstrapContext bootstrapContext,
							SessionFactoryImplementor sessionFactory) {
						metadata.getImports().put( "PersonDto", PersonDto.class.getName() );
					}
				}
		);
	}
}
