/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dirtiness;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class CustomDirtinessStrategyTest extends BaseCoreFunctionalTestCase {
	private static final String INITIAL_NAME = "thing 1";
	private static final String SUBSEQUENT_NAME = "thing 2";

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.getProperties().put( AvailableSettings.CUSTOM_ENTITY_DIRTINESS_STRATEGY, Strategy.INSTANCE );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Thing.class };
	}

	@Test
	public void testOnlyCustomStrategy() {
		Session session = openSession();
		session.beginTransaction();
		Thing t = new Thing( INITIAL_NAME );
		session.persist( t );
		Long id = t.getId();
		session.getTransaction().commit();
		session.close();

		Strategy.INSTANCE.resetState();

		session = openSession();
		session.beginTransaction();
		Thing thing = session.get( Thing.class, id );
		thing.setName( SUBSEQUENT_NAME );
		session.getTransaction().commit();
		session.close();

		assertEquals( 1, Strategy.INSTANCE.canDirtyCheckCount );
		assertEquals( 1, Strategy.INSTANCE.isDirtyCount );
		assertEquals( 1, Strategy.INSTANCE.resetDirtyCount );
		assertEquals( 1, Strategy.INSTANCE.findDirtyCount );

		session = openSession();
		session.beginTransaction();
		thing = session.get( Thing.class, id );
		assertEquals( SUBSEQUENT_NAME, thing.getName() );
		session.remove( thing );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testCustomStrategyWithFlushInterceptor() {
		Session session = openSession();
		session.beginTransaction();
		Thing t = new Thing( INITIAL_NAME );
		session.persist( t );
		Long id = t.getId();
		session.getTransaction().commit();
		session.close();

		Strategy.INSTANCE.resetState();

		session = sessionWithInterceptor().openSession();
		session.beginTransaction();
		Thing thing = session.get( Thing.class, id );
		thing.setName( SUBSEQUENT_NAME );
		session.getTransaction().commit();
		session.close();

		// As we used an interceptor, the custom strategy should have been called twice to find dirty properties
		assertEquals( 1, Strategy.INSTANCE.canDirtyCheckCount );
		assertEquals( 1, Strategy.INSTANCE.isDirtyCount );
		assertEquals( 1, Strategy.INSTANCE.resetDirtyCount );
		assertEquals( 2, Strategy.INSTANCE.findDirtyCount );

		session = openSession();
		session.beginTransaction();
		thing = session.get( Thing.class, id );
		assertEquals( SUBSEQUENT_NAME, thing.getName() );
		session.remove( thing );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testOnlyCustomStrategyConsultedOnNonDirty() {
		Session session = openSession();
		session.beginTransaction();
		Thing t = new Thing( INITIAL_NAME );
		session.persist( t );
		Long id = t.getId();
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		Thing thing = session.get( Thing.class, id );
		// lets change the name
		thing.setName( SUBSEQUENT_NAME );
		assertTrue( Strategy.INSTANCE.isDirty( thing, null, null ) );
		// but fool the dirty map
		thing.changedValues.clear();
		assertFalse( Strategy.INSTANCE.isDirty( thing, null, null ) );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		thing = session.get( Thing.class, id );
		assertEquals( INITIAL_NAME, thing.getName() );
		session.createQuery( "delete Thing" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	private SessionBuilder sessionWithInterceptor() {
		return sessionFactory().unwrap( SessionFactory.class )
				.withOptions()
				.interceptor( OnFlushDirtyInterceptor.INSTANCE );
	}

	public static class Strategy implements CustomEntityDirtinessStrategy {
		public static final Strategy INSTANCE = new Strategy();

		int canDirtyCheckCount = 0;

		@Override
		public boolean canDirtyCheck(Object entity, EntityPersister persister, Session session) {
			canDirtyCheckCount++;
			System.out.println( "canDirtyCheck called" );
			return entity instanceof Thing;
		}

		int isDirtyCount = 0;

		@Override
		public boolean isDirty(Object entity, EntityPersister persister, Session session) {
			isDirtyCount++;
			System.out.println( "isDirty called" );
			return ! ((Thing) entity).changedValues.isEmpty();
		}

		int resetDirtyCount = 0;

		@Override
		public void resetDirty(Object entity, EntityPersister persister, Session session) {
			resetDirtyCount++;
			System.out.println( "resetDirty called" );
			((Thing) entity).changedValues.clear();
		}

		int findDirtyCount = 0;

		@Override
		public void findDirty(final Object entity, EntityPersister persister, Session session, DirtyCheckContext dirtyCheckContext) {
			findDirtyCount++;
			System.out.println( "findDirty called" );
			dirtyCheckContext.doDirtyChecking(
					attributeInformation -> ((Thing) entity).changedValues.containsKey( attributeInformation.getName() )
			);
		}

		void resetState() {
			canDirtyCheckCount = 0;
			isDirtyCount = 0;
			resetDirtyCount = 0;
			findDirtyCount = 0;
		}
	}


	public static class OnFlushDirtyInterceptor implements Interceptor {
		private static final OnFlushDirtyInterceptor INSTANCE = new OnFlushDirtyInterceptor();

		@Override
		public boolean onFlushDirty(
				Object entity,
				Object id,
				Object[] currentState,
				Object[] previousState,
				String[] propertyNames,
				Type[] types) {
			// Tell Hibernate ORM we did change the entity state, which should trigger another dirty check
			return true;
		}
	}
}
