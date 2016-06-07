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
package org.hibernate.osgitest;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * @author Brett Meyer
 */

public class HibernateUtil {

	private EntityManagerFactory emf;

	public EntityManager getEntityManager() {
		return getEntityManagerFactory().createEntityManager();
	}

	private EntityManagerFactory getEntityManagerFactory() {
		if ( emf == null ) {
			Bundle thisBundle = FrameworkUtil.getBundle( HibernateUtil.class );
			// Could get this by wiring up OsgiTestBundleActivator as well.
			BundleContext context = thisBundle.getBundleContext();

			ServiceReference serviceReference = context.getServiceReference( PersistenceProvider.class.getName() );
			PersistenceProvider persistenceProvider = (PersistenceProvider) context.getService( serviceReference );

			emf = persistenceProvider.createEntityManagerFactory( "unmanaged-jpa", null );
		}
		return emf;
	}
}
