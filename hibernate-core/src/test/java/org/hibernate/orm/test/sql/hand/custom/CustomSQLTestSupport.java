/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand.custom;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.sql.hand.Employment;
import org.hibernate.orm.test.sql.hand.ImageHolder;
import org.hibernate.orm.test.sql.hand.Organization;
import org.hibernate.orm.test.sql.hand.Person;
import org.hibernate.orm.test.sql.hand.TextHolder;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Abstract test case defining tests for the support for user-supplied (aka
 * custom) insert, update, delete SQL.
 *
 * @author Steve Ebersole
 */
@DomainModel(
		overrideCacheStrategy = false
)
@SessionFactory
public abstract class CustomSQLTestSupport {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testHandSQL(SessionFactoryScope scope) {
		Organization ifa = new Organization( "IFA" );
		Object orgId = scope.fromTransaction(
				session -> {
					Organization jboss = new Organization( "JBoss" );
					Person gavin = new Person( "Gavin" );
					Employment emp = new Employment( gavin, jboss, "AU" );
					session.persist( jboss );
					Object oId = jboss.getId();
					session.persist( ifa );
					session.persist( gavin );
					session.persist( emp );
					Person christian = new Person( "Christian" );
					session.persist( christian );
					Employment emp2 = new Employment( christian, jboss, "EU" );
					session.persist( emp2 );
					return oId;
				}
		);

		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		sessionFactory.getCache().evictEntityData( Organization.class );
		sessionFactory.getCache().evictEntityData( Person.class );
		sessionFactory.getCache().evictEntityData( Employment.class );

		scope.inTransaction(
				session -> {
					Organization jboss = session.get( Organization.class, orgId );
					assertThat( jboss.getEmployments() ).hasSize( 2 );
					assertThat( jboss.getName() ).isEqualTo( "JBOSS" );
					Employment emp = jboss.getEmployments().iterator().next();
					Person gavin = emp.getEmployee();
					assertThat( gavin.getName() ).isEqualTo( "GAVIN" );
					assertThat( session.getCurrentLockMode( gavin ) ).isEqualTo( LockMode.PESSIMISTIC_WRITE );
					emp.setEndDate( new Date() );
					Employment emp3 = new Employment( gavin, jboss, "US" );
					session.persist( emp3 );
				}
		);

		scope.inTransaction(
				session -> {
					Iterator itr = session.getNamedQuery( "allOrganizationsWithEmployees" ).list().iterator();
					assertThat( itr.hasNext() ).isTrue();
					Organization o = (Organization) itr.next();
					assertThat( o.getEmployments() ).hasSize( 3 );
					Iterator itr2 = o.getEmployments().iterator();
					while ( itr2.hasNext() ) {
						Employment e = (Employment) itr2.next();
						session.remove( e );
					}
					itr2 = o.getEmployments().iterator();
					while ( itr2.hasNext() ) {
						Employment e = (Employment) itr2.next();
						session.remove( e.getEmployee() );
					}
					session.remove( o );
					assertThat( itr.hasNext() ).isFalse();
					session.remove( ifa );
				}
		);
	}

	@Test
	public void testTextProperty(SessionFactoryScope scope) {
		String d = buildLongString( 15000, 'a' );
		TextHolder h = new TextHolder( d );
		scope.inTransaction(
				session -> {
					session.persist( h );
				}
		);

		String description = buildLongString( 15000, 'b' );
		scope.inTransaction(
				session -> {
					TextHolder holder = session.get( TextHolder.class, h.getId() );
					assertThat( holder.getDescription() ).isEqualTo( d );
					holder.setDescription( description );
					session.persist( holder );
				}
		);

		scope.inTransaction(
				session -> {
					TextHolder holder = session.get( TextHolder.class, h.getId() );
					assertThat( holder.getDescription() ).isEqualTo( description );
					session.remove( holder );
				}
		);
	}

	@Test
	public void testImageProperty(SessionFactoryScope scope) {
		// Make sure the last byte is non-zero as Sybase cuts that off
		byte[] p = buildLongByteArray( 14999, true );
		ImageHolder h = new ImageHolder( p );
		scope.inTransaction(
				session -> {
					session.persist( h );
				}
		);

		byte[] photo = buildLongByteArray( 15000, false );
		scope.inTransaction(
				session -> {
					ImageHolder holder = session.get( ImageHolder.class, h.getId() );
					assertThat( holder.getPhoto() ).isEqualTo( p );
					holder.setPhoto( photo );
					session.persist( holder );
				}
		);

		scope.inTransaction(
				session -> {
					ImageHolder holder = session.get( ImageHolder.class, h.getId() );
					assertThat( holder.getPhoto() ).isEqualTo( photo );
					session.remove( h );
				}
		);
	}

	private String buildLongString(int size, char baseChar) {
		StringBuilder buff = new StringBuilder();
		for ( int i = 0; i < size; i++ ) {
			buff.append( baseChar );
		}
		return buff.toString();
	}

	private byte[] buildLongByteArray(int size, boolean on) {
		byte[] data = new byte[size];
		data[0] = mask( on );
		for ( int i = 0; i < size; i++ ) {
			data[i] = mask( on );
			on = !on;
		}
		return data;
	}

	private byte mask(boolean on) {
		return on ? (byte) 1 : (byte) 0;
	}
}
