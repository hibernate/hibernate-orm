/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.cdi;

import javax.enterprise.inject.spi.BeanManager;
import java.util.Map;

import org.jboss.arquillian.container.weld.ee.embedded_1_1.mock.TestContainer;
import org.jboss.weld.bootstrap.api.Environments;

import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

/**
 * @author Steve Ebersole
 */
public abstract class BaseCDIIntegrationTest extends BaseEntityManagerFunctionalTestCase {
	private TestContainer testContainer;

	@Override
	@SuppressWarnings("unchecked")
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );

		testContainer = new TestContainer( getCdiBeans() );
		testContainer.getBootstrap().startContainer( Environments.SE, testContainer.getDeployment() );
		testContainer.getBootstrap().startInitialization();
		testContainer.getBootstrap().deployBeans();
		testContainer.getBootstrap().validateBeans().endInitialization();
		options.put( AvailableSettings.CDI_BEAN_MANAGER, getBeanManager() );
	}

	protected BeanManager getBeanManager() {
		return testContainer.getBeanManager( testContainer.getDeployment().getBeanDeploymentArchives().iterator().next() );
	}

	public abstract Class[] getCdiBeans();

	@Override
	public void releaseResources() {
		super.releaseResources(); // closes the EMF

		testContainer.stopContainer();
	}
}
