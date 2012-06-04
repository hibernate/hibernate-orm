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

import org.hibernate.*;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.type.Type;
import org.junit.Test;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;

import static org.junit.Assert.*;

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
		return new Class[] { Thing.class, ChildThing.class, DynamicallyUpdatedThing.class };
	}

	@Test
	public void testOnlyCustomStrategy() {
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
		thing.getChangedValues().clear();
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

	@Test
	@TestForIssue(jiraKey = "HHH-7366")
	@FailureExpected(jiraKey = "HHH-7366")
	public void testCascadeWithNullableFKReference() throws Exception {
		Session session = openSession();
		session.beginTransaction();
		Thing thing = new Thing( INITIAL_NAME );
		thing.setChildren( new HashSet<ChildThing>() );

		//set up a circular reference to ChildThing instances to one another
		ChildThing currentChild;
		ChildThing firstChild = new ChildThing( thing, null );
		ChildThing previousChild = firstChild;
		for ( int index = 0; index < 5; index++ ) {
			currentChild = new ChildThing( thing, previousChild );
			currentChild.setNullableFKReference( previousChild );
			thing.getChildren().add( currentChild );
			previousChild = currentChild;
		}
		firstChild.setNullableFKReference( previousChild );
		thing.getChildren().add( firstChild );

		Long id = ( Long ) session.save( thing );

		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();

		thing = ( Thing ) session.get( Thing.class, id );
		boolean nullReferenceFound = false;
		for ( ChildThing childThing : thing.getChildren() ) {
			//check to see that the reference initially nulled out is eventually saved
			if ( childThing.getNullableFKReference() == null ) {
				nullReferenceFound = true;
				break;
			}
		}

		if ( nullReferenceFound ) {
			fail( "Nullable FK reference wasn't saved." );
		}
		session.delete( thing );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7366")
	@FailureExpected(jiraKey = "HHH-7366")
	public void testInterceptedValueIsUpdated() {
		Session session = openSession();
		session.beginTransaction();
		Long id = ( Long ) session.save( new DynamicallyUpdatedThing( INITIAL_NAME ) );
		session.getTransaction().commit();
		session.close();

		UpdateInterceptor updateInterceptor = new UpdateInterceptor();
		session = openSession( updateInterceptor );
		session.beginTransaction();
		DynamicallyUpdatedThing dynamicallyUpdatedThing = ( DynamicallyUpdatedThing ) session.get( DynamicallyUpdatedThing.class, id );
		dynamicallyUpdatedThing.setName( SUBSEQUENT_NAME );
		session.getTransaction().commit();
		session.close();
		assertTrue( "Update interceptor should have been called", updateInterceptor.interceptorCalled );

		session = openSession();
		session.beginTransaction();
		dynamicallyUpdatedThing = ( DynamicallyUpdatedThing ) session.get( DynamicallyUpdatedThing.class, id );
		assertEquals( SUBSEQUENT_NAME, dynamicallyUpdatedThing.getName() );
		assertNotNull( dynamicallyUpdatedThing.getMutableProperty() );
		session.delete( dynamicallyUpdatedThing );
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
			return CustomDirtyCheckable.class.isInstance( entity );
		}

		int isDirtyCount = 0;

		@Override
		public boolean isDirty(Object entity, EntityPersister persister, Session session) {
			isDirtyCount++;
			System.out.println( "isDirty called" );
			return ! CustomDirtyCheckable.class.cast( entity ).getChangedValues().isEmpty();
		}

		int resetDirtyCount = 0;

		@Override
		public void resetDirty(Object entity, EntityPersister persister, Session session) {
			resetDirtyCount++;
			System.out.println( "resetDirty called" );
			CustomDirtyCheckable.class.cast( entity ).getChangedValues().clear();
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
							return CustomDirtyCheckable.class.cast( entity ).getChangedValues().containsKey( attributeInformation.getName() );
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
	
	public static class UpdateInterceptor extends EmptyInterceptor {
		boolean interceptorCalled;
		
		@Override
		public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
			for (int i = 0; i < propertyNames.length; i++ ){
				String propertyName = propertyNames[i];
				if ( "mutableProperty".equals( propertyName ) ){
					interceptorCalled = true;
					currentState[i] = new Date();
					return true;
				}
			}
			return false;
		}
	}
}
