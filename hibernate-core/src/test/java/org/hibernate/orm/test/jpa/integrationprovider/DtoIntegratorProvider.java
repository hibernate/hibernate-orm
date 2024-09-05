/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
