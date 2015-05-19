/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.osgi.test.client;

import java.util.Hashtable;

import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.ServiceContributor;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author Brett Meyer
 * @author Steve Ebersole
 */
public class OsgiTestActivator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		// register example extension point services
		context.registerService( Integrator.class, new TestIntegrator(), new Hashtable() );
		context.registerService( StrategyRegistrationProvider.class, new TestStrategyRegistrationProvider(), new Hashtable() );
		context.registerService( TypeContributor.class, new TestTypeContributor(), new Hashtable() );
		context.registerService( ServiceContributor.class, new SomeServiceContributor(), new Hashtable() );
	}

	@Override
	public void stop(BundleContext context) throws Exception {

	}

}
