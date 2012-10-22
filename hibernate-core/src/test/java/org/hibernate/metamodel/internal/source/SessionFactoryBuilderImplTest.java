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
package org.hibernate.metamodel.internal.source;

import java.io.Serializable;
import java.util.Iterator;

import org.junit.Test;

import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.EntityMode;
import org.hibernate.Interceptor;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.SessionFactoryBuilder;
import org.hibernate.metamodel.internal.MetadataImpl;
import org.hibernate.metamodel.internal.SessionFactoryBuilderImpl;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.type.Type;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class SessionFactoryBuilderImplTest extends BaseUnitTestCase {

	@Test
	public void testGettingSessionFactoryBuilder() {
		SessionFactoryBuilder sessionFactoryBuilder = getSessionFactoryBuilder();
		assertNotNull( sessionFactoryBuilder );
		assertTrue( SessionFactoryBuilderImpl.class.isInstance( sessionFactoryBuilder ) );
	}

	@Test
	public void testBuildSessionFactoryWithDefaultOptions() {
		SessionFactoryBuilder sessionFactoryBuilder = getSessionFactoryBuilder();
		SessionFactory sessionFactory = sessionFactoryBuilder.build();
		assertSame( EmptyInterceptor.INSTANCE, sessionFactory.getSessionFactoryOptions().getInterceptor() );
		assertTrue( EntityNotFoundDelegate.class.isInstance(
				sessionFactory.getSessionFactoryOptions().getEntityNotFoundDelegate()
		) );
		sessionFactory.close();
	}

	@Test
	public void testBuildSessionFactoryWithUpdatedOptions() {
		SessionFactoryBuilder sessionFactoryBuilder = getSessionFactoryBuilder();
		Interceptor interceptor = new AnInterceptor();
		EntityNotFoundDelegate entityNotFoundDelegate = new EntityNotFoundDelegate() {
			@Override
			public void handleEntityNotFound(String entityName, Serializable id) {
				throw new ObjectNotFoundException( id, entityName );
			}
		};
		sessionFactoryBuilder.with( interceptor );
		sessionFactoryBuilder.with( entityNotFoundDelegate );
		SessionFactory sessionFactory = sessionFactoryBuilder.build();
		assertSame( interceptor, sessionFactory.getSessionFactoryOptions().getInterceptor() );
		assertSame( entityNotFoundDelegate, sessionFactory.getSessionFactoryOptions().getEntityNotFoundDelegate() );
		sessionFactory.close();
	}

	private SessionFactoryBuilder getSessionFactoryBuilder() {
		MetadataSources sources = new MetadataSources( new StandardServiceRegistryBuilder().build() );
		sources.addAnnotatedClass( SimpleEntity.class );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();
		return  metadata.getSessionFactoryBuilder();
	}

	private static class AnInterceptor implements Interceptor {
		private static final Interceptor INSTANCE = EmptyInterceptor.INSTANCE;

		@Override
		public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types)
				throws CallbackException {
			return INSTANCE.onLoad( entity, id, state, propertyNames, types );
		}

		@Override
		public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types)
				throws CallbackException {
			return INSTANCE.onFlushDirty( entity, id, currentState, previousState, propertyNames, types );
		}

		@Override
		public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types)
				throws CallbackException {
			return INSTANCE.onSave( entity, id, state, propertyNames, types );
		}

		@Override
		public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types)
				throws CallbackException {
			INSTANCE.onDelete( entity, id, state, propertyNames, types );
		}

		@Override
		public void onCollectionRecreate(Object collection, Serializable key) throws CallbackException {
			INSTANCE.onCollectionRecreate( collection, key );
		}

		@Override
		public void onCollectionRemove(Object collection, Serializable key) throws CallbackException {
			INSTANCE.onCollectionRemove( collection, key );
		}

		@Override
		public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
			INSTANCE.onCollectionUpdate( collection, key );
		}

		@Override
		public void preFlush(Iterator entities) throws CallbackException {
			INSTANCE.preFlush( entities );
		}

		@Override
		public void postFlush(Iterator entities) throws CallbackException {
			INSTANCE.postFlush( entities );
		}

		@Override
		public Boolean isTransient(Object entity) {
			return INSTANCE.isTransient( entity );
		}

		@Override
		public int[] findDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
			return INSTANCE.findDirty( entity, id, currentState, previousState, propertyNames, types );
		}

		@Override
		public Object instantiate(String entityName, EntityMode entityMode, Serializable id)
				throws CallbackException {
			return INSTANCE.instantiate( entityName, entityMode, id );
		}

		@Override
		public String getEntityName(Object object) throws CallbackException {
			return INSTANCE.getEntityName( object );
		}

		@Override
		public Object getEntity(String entityName, Serializable id) throws CallbackException {
			return INSTANCE.getEntity( entityName, id );
		}

		@Override
		public void afterTransactionBegin(Transaction tx) {
			INSTANCE.afterTransactionBegin( tx );
		}

		@Override
		public void beforeTransactionCompletion(Transaction tx) {
			INSTANCE.beforeTransactionCompletion( tx );
		}

		@Override
		public void afterTransactionCompletion(Transaction tx) {
			INSTANCE.afterTransactionCompletion( tx );
		}

		@Override
		public String onPrepareStatement(String sql) {
			return INSTANCE.onPrepareStatement( sql );
		}
	}
}


