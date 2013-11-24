/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.EntityManagerFactory;

import org.hibernate.HibernateException;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

/**
 * An internal registry of all {@link EntityManagerFactoryImpl} instances for the same
 * classloader as this class.
 *
 * This registry is used for serialization/deserialization of entity managers.
 *
 * @author Scott Marlow
 */
public class EntityManagerFactoryRegistry {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			EntityManagerFactoryRegistry.class.getName()
	);

	public static final EntityManagerFactoryRegistry INSTANCE = new EntityManagerFactoryRegistry();

	private final ConcurrentHashMap<String, Set<EntityManagerFactory>> entityManagerFactoryMap = new ConcurrentHashMap<String, Set<EntityManagerFactory>>();

	public EntityManagerFactoryRegistry() {
		LOG.debugf( "Initializing EntityManagerFactoryRegistry : %s", this );
	}

	/**
	 * register the specified entity manager factory
	 *
	 * @param name to register the passed entity manager factory
	 * @param entityManagerFactory
	 */
	public void addEntityManagerFactory(String name, EntityManagerFactory entityManagerFactory) {
		LOG.debugf( "Registering EntityManagerFactory: %s ", name );
		if (name == null) { // allow unit tests that don't specify the pu name to succeed
			LOG.tracef( "not registering EntityManagerFactory because name is null");
			return;
		}
		Set<EntityManagerFactory> entityManagerFactorySet = new HashSet<EntityManagerFactory>();
		entityManagerFactorySet.add(entityManagerFactory);
		Set<EntityManagerFactory> previous = entityManagerFactoryMap.putIfAbsent( name, entityManagerFactorySet);

		// if already added under 'name'.  Where 'name' could be session factory name, pu name or uuid (previous
		// will be null).  We will give a warning that an EntityManagerFactory is created with the same name
		// as is already used for a different EMF.  The best way to avoid the warning is to specify the AvailableSettings.SESSION_FACTORY_NAME
		// with a unique name.
		if (previous != null) {
			LOG.entityManagerFactoryAlreadyRegistered(name, AvailableSettings.ENTITY_MANAGER_FACTORY_NAME);
			boolean done = false;
			while( !done) {
				synchronized (previous) {
					if (entityManagerFactoryMap.get(name) == previous) {  // compare and set EMF if same
						previous.add(entityManagerFactory);
						done = true;
					}
					else {												// else it was removed or a new set added
						previous = entityManagerFactoryMap.get(name);	// get the set added by another thread
						if (null == previous) {							// or add it here if not
							entityManagerFactoryMap.putIfAbsent( name, new HashSet<EntityManagerFactory>());
							previous = entityManagerFactoryMap.get(name);// use the current set
						}

					}
				}
			}
		}
	}

	/**
	 * remove the specified entity manager factory from the EntityManagerFactoryRegistry
	 * @param name
	 * @param entityManagerFactory
	 * @throws HibernateException if the specified entity manager factory could not be found in the registry
	 */
	public void removeEntityManagerFactory(String name, EntityManagerFactory entityManagerFactory) throws HibernateException {
		LOG.debugf( "Remove: name=%s", name );

		if (name == null) { // allow unit tests that don't specify the pu name to succeed
			LOG.tracef( "not removing EntityManagerFactory from registry because name is null");
			return;
		}

		Set<EntityManagerFactory> entityManagerFactorySet = entityManagerFactoryMap.get(name);
		if (entityManagerFactorySet == null) {
			throw new HibernateException( "registry does not contain entity manager factory: " + name);
		}
		synchronized (entityManagerFactorySet) {
			boolean removed = entityManagerFactorySet.remove(entityManagerFactory);

			if (entityManagerFactorySet.size() == 0) {
				entityManagerFactoryMap.remove( name );
			}
		}
	}

	/**
	 * Lookup the specified entity manager factory by name
	 * @param name
	 * @return
	 * @throws HibernateException if entity manager factory is not found or if more than one
	 * entity manager factory was registered with name.
	 */
	public EntityManagerFactory getNamedEntityManagerFactory(String name) throws HibernateException {
		LOG.debugf( "Lookup: name=%s", name );
		Set<EntityManagerFactory> entityManagerFactorySet = entityManagerFactoryMap.get(name);

		if (entityManagerFactorySet == null) {
			throw new HibernateException( "registry does not contain entity manager factory: " + name);
		}
		synchronized (entityManagerFactorySet) {
			if (entityManagerFactorySet.size() > 1) {
				throw new HibernateException( "registry contains more than one (" + entityManagerFactorySet.size()+ ") entity manager factories: " + name);
			}
			return entityManagerFactorySet.iterator().next();
		}
	}
}
