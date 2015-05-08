/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
