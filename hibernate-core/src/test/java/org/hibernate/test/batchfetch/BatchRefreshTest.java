/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.batchfetch;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.HibernateProxyHelper;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Karl von Randow
 */
public class BatchRefreshTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { BatchLoadableEntity.class, BatchLoadableContainerEntity.class, BatchLoadableChildEntity.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testBatchRefresh() {
		Session s = openSession();
		s.beginTransaction();
		int size = 32+14;
		for ( int i = 0; i < size; i++ ) {
			s.save( new BatchLoadableEntity( i ) );
		}
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		
		// load them all as proxies
		for ( int i = 0; i < size; i++ ) {
			BatchLoadableEntity entity = (BatchLoadableEntity) s.load( BatchLoadableEntity.class, i );
			assertFalse( Hibernate.isInitialized( entity ) );
		}
		
		BatchLoadableEntity entity = (BatchLoadableEntity) s.load( BatchLoadableEntity.class, 0 );
		Hibernate.initialize(entity);
		
		/* Entity must be initialised otherwise refresh is a noop - we unwrap the proxies so we can see the
		 * underlying entities. Sometimes we are not dealing with proxies.
		 */		
		entity = (BatchLoadableEntity) ((HibernateProxy)entity).writeReplace();
		
		assertTrue( Hibernate.isInitialized( entity ) );
		
		s.refresh(entity, LockOptions.UPGRADE);
		
		BatchLoadableEntity entity2 = (BatchLoadableEntity) s.get(BatchLoadableEntity.class, 0);
		entity2 = (BatchLoadableEntity) ((HibernateProxy)entity2).writeReplace();
		
		/* Check that the entity we get back from the session is the same as the one we had before */
		assertTrue(entity == entity2);
		
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete BatchLoadableEntity" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}
	
	@Test
	@SuppressWarnings( {"unchecked"})
	public void testBatchRefreshWithCollections() {
		Session s = openSession();
		s.beginTransaction();
		int size = 32 + 14;
		int childSize = 2;
		for ( int i = 0; i < size; i++ ) {
			BatchLoadableContainerEntity containerEntity = new BatchLoadableContainerEntity( i );
			for ( int j = 0; j < childSize; j++ ) {
				BatchLoadableChildEntity childEntity = new BatchLoadableChildEntity(i * childSize + j);
				containerEntity.getChildren().add(childEntity);
			}
			s.save( containerEntity );
		}
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		
		// load them all as proxies
		for ( int i = 0; i < size; i++ ) {
			BatchLoadableContainerEntity entity = (BatchLoadableContainerEntity) s.load( BatchLoadableContainerEntity.class, i );
			assertFalse( Hibernate.isInitialized( entity ) );
		}
		
		BatchLoadableContainerEntity entity = (BatchLoadableContainerEntity) s.load( BatchLoadableContainerEntity.class, 0 );
		Hibernate.initialize(entity);
		
		/* Entity must be initialised otherwise refresh is a noop - we unwrap the proxies so we can see the
		 * underlying entities. Sometimes we are not dealing with proxies.
		 */		
		entity = (BatchLoadableContainerEntity) ((HibernateProxy)entity).writeReplace();
		
		assertTrue( Hibernate.isInitialized( entity ) );
		
		s.refresh(entity, LockOptions.UPGRADE);
		
		try {
			entity.getChildren().add(new BatchLoadableChildEntity(size * childSize + 1));
		} catch (LazyInitializationException e) {
			/* The "children" PersistentBag will have a null session and cannot be initialized if session.refresh has loaded
			 * a new entity rather than repopulating the given one
			 */
			assertTrue("entity.getChildren() failed to initialise", false);
		}
		
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete BatchLoadableContainerEntity" ).executeUpdate();
		s.createQuery( "delete BatchLoadableChildEntity" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}
	
}

