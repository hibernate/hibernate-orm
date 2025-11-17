/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance.joined;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				File.class,
				Folder.class,
				Document.class,
				SymbolicLink.class,
				ProgramExecution.class,
				Clothing.class,
				Sweater.class,
				EventInformation.class,
				Alarm.class,
				Client.class,
				Account.class,
				Company.class
		}
)
@SessionFactory
public class JoinedSubclassTest {

	@Test
	public void testDefault(SessionFactoryScope scope) {
		File doc = new Document( "Enron Stuff To Shred", 1000 );
		Folder folder = new Folder( "Enron" );
		scope.inTransaction(
				s -> {
					s.persist( doc );
					s.persist( folder );
				}
		);
		scope.inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<File> criteria = criteriaBuilder.createQuery( File.class );
					criteria.from( File.class );
					List<File> result = s.createQuery( criteria ).list();
//					List result = s.createCriteria( File.class ).list();
					assertNotNull( result );
					assertEquals( 2, result.size() );
					File f2 = result.get( 0 );
					checkClassType( f2, doc, folder );
					f2 = result.get( 1 );
					checkClassType( f2, doc, folder );
				}
		);
	}

	@Test
	public void testManyToOneOnAbstract(SessionFactoryScope scope) {
		Folder f = new Folder();
		f.setName( "data" );
		ProgramExecution remove = new ProgramExecution();
		remove.setAction( "remove" );
		remove.setAppliesOn( f );

		scope.inTransaction(
				session -> {
					session.persist( f );
					session.persist( remove );
				}
		);

		scope.inTransaction(
				session -> {
					ProgramExecution programExecution = session.get( ProgramExecution.class, remove.getId() );
					assertNotNull( programExecution );
					assertNotNull( programExecution.getAppliesOn().getName() );
				}
		);
	}

	@Test
	public void testJoinedAbstractClass(SessionFactoryScope scope) {
		Sweater sw = new Sweater();
		sw.setColor( "Black" );
		sw.setSize( 2 );
		sw.setSweat( true );

		scope.inTransaction(
				session -> session.persist( sw )

		);

		scope.inTransaction(
				session -> {
					Sweater toDelete = session.get( Sweater.class, sw.getId() );
					session.remove( toDelete );
				}
		);
	}

	@Test
	public void testInheritance(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String eventPK = "event1";
					EventInformation event = session.get( EventInformation.class, eventPK );
					if ( event == null ) {
						event = new EventInformation();
						event.setNotificationId( eventPK );
						session.persist( event );
					}
					String alarmPK = "alarm1";
					Alarm alarm = session.get( Alarm.class, alarmPK );
					if ( alarm == null ) {
						alarm = new Alarm();
						alarm.setNotificationId( alarmPK );
						alarm.setEventInfo( event );
						session.persist( alarm );
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-4250")
	public void testManyToOneWithJoinTable(SessionFactoryScope scope) {
		//HHH-4250 : @ManyToOne - @OneToMany doesn't work with @Inheritance(strategy= InheritanceType.JOINED)
		scope.inTransaction(
				session -> {
					Client c1 = new Client();
					c1.setFirstname( "Firstname1" );
					c1.setName( "Name1" );
					c1.setCode( "1234" );
					c1.setStreet( "Street1" );
					c1.setCity( "City1" );

					Account a1 = new Account();
					a1.setNumber( "1000" );
					a1.setBalance( 5000.0 );

					a1.addClient( c1 );

					session.persist( c1 );
					session.persist( a1 );

					session.flush();
					session.clear();

					c1 = session.getReference( Client.class, c1.getId() );
					assertEquals( 5000.0, c1.getAccount().getBalance(), 0.01 );

					session.flush();
					session.clear();

					a1 = session.getReference( Account.class, a1.getId() );
					Set<Client> clients = a1.getClients();
					assertEquals( 1, clients.size() );
					Iterator<Client> it = clients.iterator();
					c1 = it.next();
					assertEquals( "Name1", c1.getName() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-4240")
	public void testSecondaryTables(SessionFactoryScope scope) {
		// HHH-4240 - SecondaryTables not recognized when using JOINED inheritance
		Company company = new Company();
		company.setCustomerName( "Mama" );
		company.setCustomerCode( "123" );
		company.setCompanyName( "Mama Mia Pizza" );
		company.setCompanyAddress( "Rome" );
		scope.inTransaction(
				session ->
						session.persist( company )
		);

		scope.inTransaction(
				session -> {
					Company c = session.get( Company.class, company.getId() );
					assertEquals( "Mama", c.getCustomerName() );
					assertEquals( "123", c.getCustomerCode() );
					assertEquals( "Mama Mia Pizza", c.getCompanyName() );
					assertEquals( "Rome", c.getCompanyAddress() );
				}
		);
	}

	private void checkClassType(File fruitToTest, File f, Folder a) {
		if ( fruitToTest.getName().equals( f.getName() ) ) {
			assertFalse( fruitToTest instanceof Folder );
		}
		else if ( fruitToTest.getName().equals( a.getName() ) ) {
			assertTrue( fruitToTest instanceof Folder );
		}
		else {
			fail( "Result does not contains the previously inserted elements" );
		}
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
