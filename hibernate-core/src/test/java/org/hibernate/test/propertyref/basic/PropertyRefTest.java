/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.propertyref.basic;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class PropertyRefTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "propertyref/basic/Person.hbm.xml" };
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, "1" );
		settings.put( AvailableSettings.GENERATE_STATISTICS, "true");
	}

	@Override
	public String getCacheConcurrencyStrategy() {
		return null;
	}

	@Test
	public void testNonLazyBagKeyPropertyRef() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Person p = new Person();
		p.setName( "Steve" );
		p.setUserId( "steve" );
		p.getSystems().add( "QA" );
		p.getSystems().add( "R&D" );
		s.persist( p );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.createQuery( "from Person" ).list();
		s.clear();
		s.createSQLQuery( "select {p.*} from PROPREF_PERS {p}" )
				.addEntity( "p", Person.class.getName() )
				.list();
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List results = s.createQuery( "from Person" ).list();
		Iterator itr = results.iterator();
		while ( itr.hasNext() ) {
			s.delete( itr.next() );
		}
		t.commit();
		s.close();
	}

	@Test
	public void testManyToManyPropertyRef() {
		// prepare some test data relating to the Group->Person many-to-many association
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Person p = new Person();
		p.setName( "Steve" );
		p.setUserId( "steve" );
		s.persist( p );
		Group g = new Group();
		g.setName( "Admins" );
		g.getUsers().add( p );
		s.persist( g );
		// force a flush and detachment here to test reattachment handling of the property-ref (HHH-1531)
		t.commit();
		s.close();

		Person p2 = new Person();
		p2.setName( "Max" );
		p2.setUserId( "max" );
		g.getUsers().add( p2 );

		s = openSession();
		t = s.beginTransaction();
		s.update( g );
		t.commit();
		s.close();

		// test retrieval of the group
		s = openSession();
		t = s.beginTransaction();
		g = ( Group ) s.createQuery( "from Group g left join fetch g.users" ).uniqueResult();
		assertTrue( Hibernate.isInitialized( g.getUsers() ) );
		assertEquals( 2, g.getUsers().size() );
		s.delete( g );
		s.createQuery( "delete Person" ).executeUpdate();
		t.commit();
		s.close();
	}
	
	@Test
	public void testOneToOnePropertyRef() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Person p = new Person();
		p.setName("Steve");
		p.setUserId("steve");
		Address a = new Address();
		a.setAddress("Texas");
		a.setCountry("USA");
		p.setAddress(a);
		a.setPerson(p);
		s.save(p);
		Person p2 = new Person();
		p2.setName("Max");
		p2.setUserId("max");
		s.save(p2);
		Account act = new Account();
		act.setType('c');
		act.setUser(p2);
		p2.getAccounts().add(act);
		s.save(act);
		s.flush();
		s.clear();
		
		p = (Person) s.get( Person.class, p.getId() ); //get address reference by outer join
		p2 = (Person) s.get( Person.class, p2.getId() ); //get null address reference by outer join
		assertNull( p2.getAddress() );
		assertNotNull( p.getAddress() );
		List l = s.createQuery("from Person").list(); //pull address references for cache
		assertEquals( l.size(), 2 );
		assertTrue( l.contains(p) && l.contains(p2) );
		s.clear();
		
		l = s.createQuery("from Person p order by p.name").list(); //get address references by sequential selects
		assertEquals( l.size(), 2 );
		assertNull( ( (Person) l.get(0) ).getAddress() );
		assertNotNull( ( (Person) l.get(1) ).getAddress() );
		s.clear();
		
		l = s.createQuery("from Person p left join fetch p.address a order by a.country").list(); //get em by outer join
		assertEquals( l.size(), 2 );
		if ( ( (Person) l.get(0) ).getName().equals("Max") ) {
			assertNull( ( (Person) l.get(0) ).getAddress() );
			assertNotNull( ( (Person) l.get(1) ).getAddress() );
		}
		else {
			assertNull( ( (Person) l.get(1) ).getAddress() );
			assertNotNull( ( (Person) l.get(0) ).getAddress() );
		}
		s.clear();
		
		l = s.createQuery("from Person p left join p.accounts a").list();
		for ( int i=0; i<2; i++ ) {
			Object[] row = (Object[]) l.get(i);
			Person px = (Person) row[0];
			assertFalse( Hibernate.isInitialized( px.getAccounts() ) );
			assertTrue( px.getAccounts().size()>0 || row[1]==null );
		}
		s.clear();

		l = s.createQuery("from Person p left join fetch p.accounts a order by p.name").list();
		Person p0 = (Person) l.get(0);
		assertTrue( Hibernate.isInitialized( p0.getAccounts() ) );
		assertEquals( p0.getAccounts().size(), 1 );
		assertSame( ( (Account) p0.getAccounts().iterator().next() ).getUser(), p0 );
		Person p1 = (Person) l.get(1);
		assertTrue( Hibernate.isInitialized( p1.getAccounts() ) );
		assertEquals( p1.getAccounts().size(), 0 );
		s.clear();
		Account acc = (Account) s.createQuery("from Account a left join fetch a.user").uniqueResult();
		assertTrue( Hibernate.isInitialized(acc.getUser()) );
		assertNotNull(acc.getUser());
		assertTrue( acc.getUser().getAccounts().contains(acc) );
		
		s.createQuery("delete from Address").executeUpdate();
		s.createQuery("delete from Account").executeUpdate(); // to not break constraint violation between Person and Account
		s.createQuery("delete from Person").executeUpdate();
		
		t.commit();
		s.close();
	}

	@Test
	public void testJoinFetchPropertyRef() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Person p = new Person();
		p.setName("Steve");
		p.setUserId("steve");
		Address a = new Address();
		a.setAddress("Texas");
		a.setCountry("USA");
		p.setAddress(a);
		a.setPerson(p);
		s.save(p);

		s.flush();
		s.clear();

		sessionFactory().getStatistics().clear();

		p = (Person) s.get( Person.class, p.getId() ); //get address reference by outer join
		
		assertTrue( Hibernate.isInitialized( p.getAddress() ) );
		assertNotNull( p.getAddress() );
        assertEquals( sessionFactory().getStatistics().getPrepareStatementCount(), 1 );
        assertEquals( sessionFactory().getStatistics().getEntityFetchCount(), 0 );

		s.clear();

		sessionFactory().getStatistics().clear();

		p = (Person) s.createCriteria(Person.class)
			.setFetchMode("address", FetchMode.SELECT)
			.uniqueResult(); //get address reference by select
		
		assertTrue( Hibernate.isInitialized( p.getAddress() ) );
		assertNotNull( p.getAddress() );
        assertEquals( sessionFactory().getStatistics().getPrepareStatementCount(), 2 );
        assertEquals( sessionFactory().getStatistics().getEntityFetchCount(), 0 );

		s.createQuery("delete from Address").executeUpdate();
		s.createQuery("delete from Person").executeUpdate();
		
		t.commit();
		s.close();
	}

	@Test
	public void testForeignKeyCreation() {
		PersistentClass classMapping = metadata().getEntityBinding( Account.class.getName() );
		
		Iterator foreignKeyIterator = classMapping.getTable().getForeignKeyIterator();
		boolean found = false;
		while ( foreignKeyIterator.hasNext() ) {
			ForeignKey element = (ForeignKey) foreignKeyIterator.next();
			if(element.getReferencedEntityName().equals(Person.class.getName() ) ) {
				
				if(!element.isReferenceToPrimaryKey() ) {
					List referencedColumns = element.getReferencedColumns();
					Column column = (Column) referencedColumns.get(0);
					if(column.getName().equals("person_userid") ) {
						found = true; // extend test to include the columns
					}				
				}
			}
		}
		
		assertTrue("Property ref foreign key not found",found);
	}
}

