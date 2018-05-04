/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.jboss.as.jpa.hibernate5.service;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.spi.ServiceContributor;

/**
 * Contribute specialized Hibernate Service impls
 *
 * @author Steve Ebersole
 */
public class ServiceContributorImpl implements ServiceContributor {
	@Override
	public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.addInitiator( new CustomJtaPlatformInitiator() );
		serviceRegistryBuilder.addInitiator( new CustomRegionFactoryInitiator() );
	}
}
