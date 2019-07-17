/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.inheritance.joined;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class JoinedSubclassTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testDefault() {
		File doc = new Document( "Enron Stuff To Shred", 1000 );
		Folder folder = new Folder( "Enron" );
		inTransaction(
				s -> {
					s.persist( doc );
					s.persist( folder );
				}
		);
		inTransaction(
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
					s.delete( result.get( 0 ) );
					s.delete( result.get( 1 ) );
				}
		);
	}

	@Test
	public void testManyToOneOnAbstract() throws Exception {
		Folder f = new Folder();
		f.setName( "data" );
		ProgramExecution remove = new ProgramExecution();
		remove.setAction( "remove" );
		remove.setAppliesOn( f );

		try(Session s = openSession()) {
			Transaction tx = s.beginTransaction();
			try {
				s.persist( f );
				s.persist( remove );
				tx.commit();
				s.clear();
				tx = s.beginTransaction();
				remove = s.get( ProgramExecution.class, remove.getId() );
				assertNotNull( remove );
				assertNotNull( remove.getAppliesOn().getName() );
				s.delete( remove );
				s.delete( remove.getAppliesOn() );

				tx.commit();
			}
			catch (Exception e) {
				if ( s.getTransaction().isActive() ) {
					s.getTransaction().rollback();
				}
				throw e;
			}
		}
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

	@Test
	public void testJoinedAbstractClass() {
		Session s;
		s = openSession();
		s.getTransaction().begin();
		Sweater sw = new Sweater();
		sw.setColor( "Black" );
		sw.setSize( 2 );
		sw.setSweat( true );
		s.persist( sw );
		s.getTransaction().commit();
		s.clear();

		s = openSession();
		s.getTransaction().begin();
		sw = s.get( Sweater.class, sw.getId() );
		s.delete( sw );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testInheritance() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();
		String eventPK = "event1";
		EventInformation event = session.get( EventInformation.class, eventPK );
		if ( event == null ) {
			event = new EventInformation();
			event.setNotificationId( eventPK );
			session.persist( event );
		}
		String alarmPK = "alarm1";
		Alarm alarm = (Alarm) session.get( Alarm.class, alarmPK );
		if ( alarm == null ) {
			alarm = new Alarm();
			alarm.setNotificationId( alarmPK );
			alarm.setEventInfo( event );
			session.persist( alarm );
		}
		transaction.commit();
		session.close();
	}
	
	@Test
	@TestForIssue( jiraKey = "HHH-4250" )
	public void testManyToOneWithJoinTable() {
		//HHH-4250 : @ManyToOne - @OneToMany doesn't work with @Inheritance(strategy= InheritanceType.JOINED)
		Session s = openSession();
		Transaction tx = s.beginTransaction();
				
		Client c1 = new Client();
		c1.setFirstname("Firstname1");
		c1.setName("Name1");
		c1.setCode("1234");
		c1.setStreet("Street1");
		c1.setCity("City1");
		
		Account a1 = new Account();
		a1.setNumber("1000");
		a1.setBalance(5000.0);
		
		a1.addClient(c1);
		
		s.persist(c1);
		s.persist(a1);
	    
		s.flush();
		s.clear();
		
		c1 = s.load(Client.class, c1.getId());
		assertEquals( 5000.0, c1.getAccount().getBalance(), 0.01 );
		
		s.flush();
		s.clear();
		
		a1 = s.load(Account.class,a1.getId());
		Set<Client> clients = a1.getClients();
		assertEquals(1, clients.size());
		Iterator<Client> it = clients.iterator();
		c1 = it.next();
		assertEquals("Name1", c1.getName());
				
		tx.rollback();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-4240" )
	public void testSecondaryTables() {
		// HHH-4240 - SecondaryTables not recognized when using JOINED inheritance
		Session s = openSession();
		s.getTransaction().begin();
		
		Company company = new Company();
		company.setCustomerName("Mama");
		company.setCustomerCode("123");
		company.setCompanyName("Mama Mia Pizza");
		company.setCompanyAddress("Rome");
		
		s.persist( company );
		s.getTransaction().commit();
		s.clear();
		
		s = openSession();
		s.getTransaction().begin();
		company = (Company) s.get( Company.class, company.getId());
		assertEquals("Mama", company.getCustomerName());
		assertEquals("123", company.getCustomerCode());
		assertEquals("Mama Mia Pizza", company.getCompanyName());
		assertEquals("Rome", company.getCompanyAddress());
				
		s.delete( company );
		s.getTransaction().commit();
		s.close();
	}
	
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
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
		};
	}

}
