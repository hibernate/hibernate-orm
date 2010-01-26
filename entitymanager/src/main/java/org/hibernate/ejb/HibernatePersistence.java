/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
 * Boston, MA  02110-1301  USA\
 */
package org.hibernate.ejb;

import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.LoadState;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

import org.hibernate.ejb.util.PersistenceUtilHelper;

/**
 * Hibernate EJB3 persistence provider implementation
 *
 * @author Gavin King
 */
public class HibernatePersistence extends AvailableSettings implements PersistenceProvider {

	/**
	 * Get an entity manager factory by its entity manager name, using the specified
	 * properties (they override any found in the peristence.xml file).
	 * <p/>
	 * This is the form used in JSE environments.
	 *
	 * @param persistenceUnitName entity manager name
	 * @param properties The explicit property values
	 *
	 * @return initialized EntityManagerFactory
	 */
	public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map properties) {
		Ejb3Configuration cfg = new Ejb3Configuration();
		Ejb3Configuration configured = cfg.configure( persistenceUnitName, properties );
		return configured != null ? configured.buildEntityManagerFactory() : null;
	}

	/**
	 * Create an entity manager factory from the given persistence unit info, using the specified
	 * properties (they override any on the PUI).
	 * <p/>
	 * This is the form used by the container in a JEE environment.
	 *
	 * @param info The persistence unit information
	 * @param properties The explicit property values
	 *
	 * @return initialized EntityManagerFactory
	 */
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
		Ejb3Configuration cfg = new Ejb3Configuration();
		Ejb3Configuration configured = cfg.configure( info, properties );
		return configured != null ? configured.buildEntityManagerFactory() : null;
	}

	/**
	 * create a factory from a canonical version
	 * @deprecated
	 */
	public EntityManagerFactory createEntityManagerFactory(Map properties) {
		// This is used directly by JBoss so don't remove until further notice.  bill@jboss.org
		Ejb3Configuration cfg = new Ejb3Configuration();
		return cfg.createEntityManagerFactory( properties );
	}

	private final ProviderUtil providerUtil = new ProviderUtil() {
		public LoadState isLoadedWithoutReference(Object proxy, String property) {
			return PersistenceUtilHelper.isLoadedWithoutReference( proxy, property );
		}

		public LoadState isLoadedWithReference(Object proxy, String property) {
			return PersistenceUtilHelper.isLoadedWithReference( proxy, property );
		}

		public LoadState isLoaded(Object o) {
			return PersistenceUtilHelper.isLoaded(o);
		}
	};

	/**
	 * {@inheritDoc}
	 */
	public ProviderUtil getProviderUtil() {
		return providerUtil;
	}

}