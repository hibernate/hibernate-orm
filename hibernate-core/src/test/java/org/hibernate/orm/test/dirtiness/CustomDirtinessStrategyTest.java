/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dirtiness;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.hibernate.type.Type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {Thing.class})
@SessionFactory
@ServiceRegistry(
		settingProviders =
				@SettingProvider(
						settingName = AvailableSettings.CUSTOM_ENTITY_DIRTINESS_STRATEGY,
						provider = CustomDirtinessStrategyTest.StrategySettingProvider.class
				)
)
public class CustomDirtinessStrategyTest {
	private static final String INITIAL_NAME = "thing 1";
	private static final String SUBSEQUENT_NAME = "thing 2";

	public static class StrategySettingProvider implements SettingProvider.Provider<Strategy> {
		@Override
		public Strategy getSetting() {
			return Strategy.INSTANCE;
		}
	}

	@Test
	public void testOnlyCustomStrategy(SessionFactoryScope scope) {
		Long id = scope.fromTransaction( session -> {
			Thing t = new Thing( INITIAL_NAME );
			session.persist( t );
			return t.getId();
		} );

		Strategy.INSTANCE.resetState();

		scope.inTransaction(  session -> {
			Thing thing = session.find( Thing.class, id );
			thing.setName( SUBSEQUENT_NAME );
		} );

		assertEquals( 1, Strategy.INSTANCE.canDirtyCheckCount );
		assertEquals( 1, Strategy.INSTANCE.isDirtyCount );
		assertEquals( 2, Strategy.INSTANCE.resetDirtyCount );
		assertEquals( 1, Strategy.INSTANCE.findDirtyCount );

		scope.inTransaction(  session -> {
			Thing thing = session.find( Thing.class, id );
			assertEquals( SUBSEQUENT_NAME, thing.getName() );
			session.remove( thing );
		} );
	}

	@Test
	public void testCustomStrategyWithFlushInterceptor(SessionFactoryScope scope) {
		Long id = scope.fromTransaction( session -> {
			Thing t = new Thing( INITIAL_NAME );
			session.persist( t );
			return t.getId();
		} );

		Strategy.INSTANCE.resetState();

		{
			Session session = sessionWithInterceptor(scope).openSession();
			session.beginTransaction();
			Thing thing = session.find( Thing.class, id );
			thing.setName( SUBSEQUENT_NAME );
			session.getTransaction().commit();
			session.close();
		}

		// As we used an interceptor, the custom strategy should have been called twice to find dirty properties
		assertEquals( 1, Strategy.INSTANCE.canDirtyCheckCount );
		assertEquals( 1, Strategy.INSTANCE.isDirtyCount );
		assertEquals( 2, Strategy.INSTANCE.resetDirtyCount );
		assertEquals( 2, Strategy.INSTANCE.findDirtyCount );

		scope.inTransaction(  session -> {
			Thing thing = session.find( Thing.class, id );
			assertEquals( SUBSEQUENT_NAME, thing.getName() );
			session.remove( thing );
		} );
	}

	@Test
	public void testOnlyCustomStrategyConsultedOnNonDirty(SessionFactoryScope scope) {
		Long id = scope.fromTransaction( session -> {
			Thing t = new Thing( INITIAL_NAME );
			session.persist( t );
			return t.getId();
		} );

		scope.inTransaction(  session -> {
			Thing thing = session.find( Thing.class, id );
			// let's change the name
			thing.setName( SUBSEQUENT_NAME );
			assertTrue( Strategy.INSTANCE.isDirty( thing, null, null ) );
			// but fool the dirty map
			thing.changedValues.clear();
			assertFalse( Strategy.INSTANCE.isDirty( thing, null, null ) );
		} );

		scope.inTransaction(  session -> {
			Thing thing = session.find( Thing.class, id );
			assertEquals( INITIAL_NAME, thing.getName() );
			session.createMutationQuery( "delete Thing" ).executeUpdate();
		} );
	}

	private SessionBuilder sessionWithInterceptor(SessionFactoryScope scope) {
		return scope.getSessionFactory()
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
