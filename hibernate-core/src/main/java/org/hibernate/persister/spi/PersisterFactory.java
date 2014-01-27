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
package org.hibernate.persister.spi;

import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.Service;

/**
 * Contract for creating persister instances (both {@link EntityPersister} and {@link CollectionPersister} varieties).
 *
 * @author Steve Ebersole
 */
public interface PersisterFactory extends Service {

	// TODO: is it really necessary to provide Configuration to CollectionPersisters ?
	// Should it not be enough with associated class ? or why does EntityPersister's not get access to configuration ?
	//
	// The only reason I could see that Configuration gets passed to collection persisters
	// is so that they can look up the dom4j node name of the entity element in case
	// no explicit node name was applied at the collection element level.  Are you kidding me?
	// Trivial to fix then.  Just store and expose the node name on the entity persister
	// (which the collection persister looks up anyway via other means...).

	/**
	 * Create an entity persister instance.
	 *
	 * @param model The O/R mapping metamodel definition for the entity
	 * @param cacheAccessStrategy The caching strategy for this entity
	 * @param naturalIdAccessStrategy The caching strategy for accessing this entity's natural ID
	 * @param factory The session factory
	 * @param cfg The overall mapping
	 *
	 * @return An appropriate entity persister instance.
	 *
	 * @throws HibernateException Indicates a problem building the persister.
	 */
	public EntityPersister createEntityPersister(
			PersistentClass model,
			EntityRegionAccessStrategy cacheAccessStrategy,
			NaturalIdRegionAccessStrategy naturalIdAccessStrategy,
			SessionFactoryImplementor factory,
			Mapping cfg) throws HibernateException;

	/**
	 * Create an entity persister instance.
	 *
	 * @param model The O/R mapping metamodel definition for the entity
	 * @param cacheAccessStrategy The caching strategy for this entity
	 * @param factory The session factory
	 * @param cfg The overall mapping
	 *
	 * @return An appropriate entity persister instance.
	 *
	 * @throws HibernateException Indicates a problem building the persister.
	 */
	public EntityPersister createEntityPersister(
			EntityBinding model,
			EntityRegionAccessStrategy cacheAccessStrategy,
			NaturalIdRegionAccessStrategy naturalIdAccessStrategy,
			SessionFactoryImplementor factory,
			Mapping cfg) throws HibernateException;

	/**
	 * Create a collection persister instance.
	 *
	 * @param cfg The configuration
	 * @param model The O/R mapping metamodel definition for the collection
	 * @param cacheAccessStrategy The caching strategy for this collection
	 * @param factory The session factory
	 *
	 * @return An appropriate collection persister instance.
	 *
	 * @throws HibernateException Indicates a problem building the persister.
	 */
	public CollectionPersister createCollectionPersister(
			Configuration cfg,
			Collection model,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			SessionFactoryImplementor factory) throws HibernateException;

	/**
	 * Create a collection persister instance.
	 *
	 * @param metadata The metadata
	 * @param model The O/R mapping metamodel definition for the collection
	 * @param cacheAccessStrategy The caching strategy for this collection
	 * @param factory The session factory
	 *
	 * @return An appropriate collection persister instance.
	 *
	 * @throws HibernateException Indicates a problem building the persister.
	 */
	public CollectionPersister createCollectionPersister(
			MetadataImplementor metadata,
			PluralAttributeBinding model,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			SessionFactoryImplementor factory) throws HibernateException;

}
