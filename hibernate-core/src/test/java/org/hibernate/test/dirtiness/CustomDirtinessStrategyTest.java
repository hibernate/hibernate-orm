/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.dirtiness;

import org.junit.Test;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

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
		// todo : not sure why this only fails with metamodel

		Session session = openSession();
		session.beginTransaction();
		Long id = (Long) session.save( new Thing( INITIAL_NAME ) );
		session.getTransaction().commit();
		session.close();

		Strategy.INSTANCE.resetState();

		session = openSession();
		session.beginTransaction();
		Thing thing = (Thing) session.get( Thing.class, id );
		thing.setName( SUBSEQUENT_NAME );
		session.getTransaction().commit();
		session.close();

		assertEquals( 1, Strategy.INSTANCE.canDirtyCheckCount );
		assertEquals( 1, Strategy.INSTANCE.isDirtyCount );
		assertEquals( 1, Strategy.INSTANCE.resetDirtyCount );
		assertEquals( 1, Strategy.INSTANCE.findDirtyCount );

		session = openSession();
		session.beginTransaction();
		thing = (Thing) session.get( Thing.class, id );
		assertEquals( SUBSEQUENT_NAME, thing.getName() );
		session.delete( thing );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testOnlyCustomStrategyConsultedOnNonDirty() throws Exception {
		// todo : not sure why this only fails with metamodel

		Session session = openSession();
		session.beginTransaction();
		Long id = (Long) session.save( new Thing( INITIAL_NAME ) );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		Thing thing = (Thing) session.get( Thing.class, id );
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
		thing = (Thing) session.get( Thing.class, id );
		assertEquals( INITIAL_NAME, thing.getName() );
		session.createQuery( "delete Thing" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	public static class Strategy implements CustomEntityDirtinessStrategy {
		public static final Strategy INSTANCE = new Strategy();

		int canDirtyCheckCount = 0;

		@Override
		public boolean canDirtyCheck(Object entity, EntityPersister persister, Session session) {
			canDirtyCheckCount++;
			System.out.println( "canDirtyCheck called" );
			return Thing.class.isInstance( entity );
		}

		int isDirtyCount = 0;

		@Override
		public boolean isDirty(Object entity, EntityPersister persister, Session session) {
			isDirtyCount++;
			System.out.println( "isDirty called" );
			return ! Thing.class.cast( entity ).changedValues.isEmpty();
		}

		int resetDirtyCount = 0;

		@Override
		public void resetDirty(Object entity, EntityPersister persister, Session session) {
			resetDirtyCount++;
			System.out.println( "resetDirty called" );
			Thing.class.cast( entity ).changedValues.clear();
		}

		int findDirtyCount = 0;

		@Override
		public void findDirty(final Object entity, EntityPersister persister, Session session, DirtyCheckContext dirtyCheckContext) {
			findDirtyCount++;
			System.out.println( "findDirty called" );
			dirtyCheckContext.doDirtyChecking(
					new AttributeChecker() {
						@Override
						public boolean isDirty(AttributeInformation attributeInformation) {
							return Thing.class.cast( entity ).changedValues.containsKey( attributeInformation.getName() );
						}
					}
			);
		}

		void resetState() {
			canDirtyCheckCount = 0;
			isDirtyCount = 0;
			resetDirtyCount = 0;
			findDirtyCount = 0;
		}
	}

}
