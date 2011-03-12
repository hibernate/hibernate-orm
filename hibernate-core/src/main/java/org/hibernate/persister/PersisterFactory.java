/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.persister;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.collection.BasicCollectionPersister;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.OneToManyPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;

/**
 * Factory for <tt>EntityPersister</tt> and <tt>CollectionPersister</tt> instances
 *
 * @author Gavin King
 */
public final class PersisterFactory {

	//TODO: make EntityPersister *not* depend on SessionFactoryImplementor
	//interface, if possible

	// TODO : still need to make CollectionPersisters EntityMode-aware

	private PersisterFactory() {}

	private static final Class[] PERSISTER_CONSTRUCTOR_ARGS = new Class[] {
		PersistentClass.class, EntityRegionAccessStrategy.class, SessionFactoryImplementor.class, Mapping.class
	};

	// TODO: is it really neceassry to provide Configuration to CollectionPersisters ? Should it not be enough with associated class ?
	// or why does EntityPersister's not get access to configuration ?
	//
	// The only reason I could see that Configuration gets passed to collection persisters
	// is so that they can look up the dom4j node name of the entity element in case
	// no explicit node name was applied at the collection element level.  Are you kidding me?
	// Trivial to fix then.  Just store and expose the node name on the entity persister
	// (which the collection persister looks up anyway via other means...).
	private static final Class[] COLLECTION_PERSISTER_CONSTRUCTOR_ARGS = new Class[] {
			Collection.class, CollectionRegionAccessStrategy.class, Configuration.class, SessionFactoryImplementor.class
		};

	public static EntityPersister createClassPersister(
			PersistentClass model,
			EntityRegionAccessStrategy cacheAccessStrategy,
			SessionFactoryImplementor factory,
			Mapping cfg) throws HibernateException {
		Class persisterClass = model.getEntityPersisterClass();
		if ( persisterClass == null || persisterClass == SingleTableEntityPersister.class ) {
			return new SingleTableEntityPersister( model, cacheAccessStrategy, factory, cfg );
		}
		else if ( persisterClass == JoinedSubclassEntityPersister.class ) {
			return new JoinedSubclassEntityPersister( model, cacheAccessStrategy, factory, cfg );
		}
		else if ( persisterClass == UnionSubclassEntityPersister.class ) {
			return new UnionSubclassEntityPersister( model, cacheAccessStrategy, factory, cfg );
		}
		else {
			return create( persisterClass, model, cacheAccessStrategy, factory, cfg );
		}
	}

	public static CollectionPersister createCollectionPersister(
			Configuration cfg,
			Collection model,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			SessionFactoryImplementor factory) throws HibernateException {
		Class persisterClass = model.getCollectionPersisterClass();
		if ( persisterClass == null ) {
			return model.isOneToMany()
					? ( CollectionPersister ) new OneToManyPersister( model, cacheAccessStrategy, cfg, factory )
					: ( CollectionPersister ) new BasicCollectionPersister( model, cacheAccessStrategy, cfg, factory );
		}
		else {
			return create( persisterClass, cfg, model, cacheAccessStrategy, factory );
		}

	}

	private static EntityPersister create(
			Class persisterClass, 
			PersistentClass model, 
			EntityRegionAccessStrategy cacheAccessStrategy,
			SessionFactoryImplementor factory,
			Mapping cfg) throws HibernateException {
		Constructor pc;
		try {
			pc = persisterClass.getConstructor( PERSISTER_CONSTRUCTOR_ARGS );
		}
		catch ( Exception e ) {
			throw new MappingException( "Could not get constructor for " + persisterClass.getName(), e );
		}

		try {
			return (EntityPersister) pc.newInstance( new Object[] { model, cacheAccessStrategy, factory, cfg } );
		}
		catch (InvocationTargetException ite) {
			Throwable e = ite.getTargetException();
			if (e instanceof HibernateException) {
				throw (HibernateException) e;
			}
			else {
				throw new MappingException( "Could not instantiate persister " + persisterClass.getName(), e );
			}
		}
		catch (Exception e) {
			throw new MappingException( "Could not instantiate persister " + persisterClass.getName(), e );
		}
	}

	private static CollectionPersister create(
			Class persisterClass,
			Configuration cfg,
			Collection model,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			SessionFactoryImplementor factory) throws HibernateException {
		Constructor pc;
		try {
			pc = persisterClass.getConstructor( COLLECTION_PERSISTER_CONSTRUCTOR_ARGS );
		}
		catch (Exception e) {
			throw new MappingException( "Could not get constructor for " + persisterClass.getName(), e );
		}

		try {
			return (CollectionPersister) pc.newInstance( new Object[] { model, cacheAccessStrategy, cfg, factory } );
		}
		catch (InvocationTargetException ite) {
			Throwable e = ite.getTargetException();
			if (e instanceof HibernateException) {
				throw (HibernateException) e;
			}
			else {
				throw new MappingException( "Could not instantiate collection persister " + persisterClass.getName(), e );
			}
		}
		catch (Exception e) {
			throw new MappingException( "Could not instantiate collection persister " + persisterClass.getName(), e );
		}
	}


}
