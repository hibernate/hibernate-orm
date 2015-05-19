/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.cdi;

import java.util.Map;
import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.jboss.arquillian.container.weld.ee.embedded_1_1.mock.TestContainer;
import org.jboss.weld.bootstrap.api.Environments;

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
