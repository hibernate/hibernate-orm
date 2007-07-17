//$Id: SubselectFetchTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.subselectfetch;

import java.util.List;

import junit.framework.Test;

import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Property;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class SubselectFetchTest extends FunctionalTestCase {

	public SubselectFetchTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "subselectfetch/ParentChild.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( SubselectFetchTest.class );
	}
	
	public void testSubselectFetchHql() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Parent p = new Parent("foo");
		p.getChildren().add( new Child("foo1") );
		p.getChildren().add( new Child("foo2") );
		Parent q = new Parent("bar");
		q.getChildren().add( new Child("bar1") );
		q.getChildren().add( new Child("bar2") );
		q.getMoreChildren().addAll( p.getChildren() );
		s.persist(p); 
		s.persist(q);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		
		getSessions().getStatistics().clear();
		
		List parents = s.createQuery("from Parent where name between 'bar' and 'foo' order by name desc")
			.list();
		p = (Parent) parents.get(0);
		q = (Parent) parents.get(1);

		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		assertFalse( Hibernate.isInitialized( q.getChildren() ) );

		assertEquals( p.getChildren().size(), 2 );
		
		assertTrue( Hibernate.isInitialized( p.getChildren().iterator().next() ) );
				
		assertTrue( Hibernate.isInitialized( q.getChildren() ) );
		
		assertEquals( q.getChildren().size(), 2 );
		
		assertTrue( Hibernate.isInitialized( q.getChildren().iterator().next() ) );
		
		assertFalse( Hibernate.isInitialized( p.getMoreChildren() ) );
		assertFalse( Hibernate.isInitialized( q.getMoreChildren() ) );

		assertEquals( p.getMoreChildren().size(), 0 );
		
		assertTrue( Hibernate.isInitialized( q.getMoreChildren() ) );
	
		assertEquals( q.getMoreChildren().size(), 2 );
		
		assertTrue( Hibernate.isInitialized( q.getMoreChildren().iterator().next() ) );
		
		assertEquals( 3, getSessions().getStatistics().getPrepareStatementCount() );

		Child c = (Child) p.getChildren().get(0);
		c.getFriends().size();

		s.delete(p);
		s.delete(q);		

		t.commit();
		s.close();
	}
	
	public void testSubselectFetchNamedParam() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Parent p = new Parent("foo");
		p.getChildren().add( new Child("foo1") );
		p.getChildren().add( new Child("foo2") );
		Parent q = new Parent("bar");
		q.getChildren().add( new Child("bar1") );
		q.getChildren().add( new Child("bar2") );
		q.getMoreChildren().addAll( p.getChildren() );
		s.persist(p); 
		s.persist(q);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		
		getSessions().getStatistics().clear();
		
		List parents = s.createQuery("from Parent where name between :bar and :foo order by name desc")
			.setParameter("bar", "bar")
			.setParameter("foo", "foo")
			.list();
		p = (Parent) parents.get(0);
		q = (Parent) parents.get(1);

		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		assertFalse( Hibernate.isInitialized( q.getChildren() ) );

		assertEquals( p.getChildren().size(), 2 );
		
		assertTrue( Hibernate.isInitialized( p.getChildren().iterator().next() ) );
				
		assertTrue( Hibernate.isInitialized( q.getChildren() ) );
		
		assertEquals( q.getChildren().size(), 2 );
		
		assertTrue( Hibernate.isInitialized( q.getChildren().iterator().next() ) );
		
		assertFalse( Hibernate.isInitialized( p.getMoreChildren() ) );
		assertFalse( Hibernate.isInitialized( q.getMoreChildren() ) );

		assertEquals( p.getMoreChildren().size(), 0 );
		
		assertTrue( Hibernate.isInitialized( q.getMoreChildren() ) );
	
		assertEquals( q.getMoreChildren().size(), 2 );
		
		assertTrue( Hibernate.isInitialized( q.getMoreChildren().iterator().next() ) );
		
		assertEquals( 3, getSessions().getStatistics().getPrepareStatementCount() );

		Child c = (Child) p.getChildren().get(0);
		c.getFriends().size();

		s.delete(p);
		s.delete(q);		

		t.commit();
		s.close();
	}
	
	public void testSubselectFetchPosParam() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Parent p = new Parent("foo");
		p.getChildren().add( new Child("foo1") );
		p.getChildren().add( new Child("foo2") );
		Parent q = new Parent("bar");
		q.getChildren().add( new Child("bar1") );
		q.getChildren().add( new Child("bar2") );
		q.getMoreChildren().addAll( p.getChildren() );
		s.persist(p); 
		s.persist(q);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		
		getSessions().getStatistics().clear();
		
		List parents = s.createQuery("from Parent where name between ? and ? order by name desc")
			.setParameter(0, "bar")
			.setParameter(1, "foo")
			.list();
		p = (Parent) parents.get(0);
		q = (Parent) parents.get(1);

		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		assertFalse( Hibernate.isInitialized( q.getChildren() ) );

		assertEquals( p.getChildren().size(), 2 );
		
		assertTrue( Hibernate.isInitialized( p.getChildren().iterator().next() ) );
				
		assertTrue( Hibernate.isInitialized( q.getChildren() ) );
		
		assertEquals( q.getChildren().size(), 2 );
		
		assertTrue( Hibernate.isInitialized( q.getChildren().iterator().next() ) );
		
		assertFalse( Hibernate.isInitialized( p.getMoreChildren() ) );
		assertFalse( Hibernate.isInitialized( q.getMoreChildren() ) );

		assertEquals( p.getMoreChildren().size(), 0 );
		
		assertTrue( Hibernate.isInitialized( q.getMoreChildren() ) );
	
		assertEquals( q.getMoreChildren().size(), 2 );
		
		assertTrue( Hibernate.isInitialized( q.getMoreChildren().iterator().next() ) );
		
		assertEquals( 3, getSessions().getStatistics().getPrepareStatementCount() );

		Child c = (Child) p.getChildren().get(0);
		c.getFriends().size();

		s.delete(p);
		s.delete(q);		

		t.commit();
		s.close();
	}
	
	public void testSubselectFetchWithLimit() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Parent p = new Parent("foo");
		p.getChildren().add( new Child("foo1") );
		p.getChildren().add( new Child("foo2") );
		Parent q = new Parent("bar");
		q.getChildren().add( new Child("bar1") );
		q.getChildren().add( new Child("bar2") );
		Parent r = new Parent("aaa");
		r.getChildren().add( new Child("aaa1") );
		s.persist(p); 
		s.persist(q);
		s.persist(r);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		
		getSessions().getStatistics().clear();
		
		List parents = s.createQuery("from Parent order by name desc")
			.setMaxResults(2)
			.list();
		p = (Parent) parents.get(0);
		q = (Parent) parents.get(1);
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		assertFalse( Hibernate.isInitialized( p.getMoreChildren() ) );
		assertFalse( Hibernate.isInitialized( q.getChildren() ) );
		assertFalse( Hibernate.isInitialized( q.getMoreChildren() ) );
		assertEquals( p.getMoreChildren().size(), 0 );
		assertEquals( p.getChildren().size(), 2 );
		assertTrue( Hibernate.isInitialized( q.getChildren() ) );
		assertTrue( Hibernate.isInitialized( q.getMoreChildren() ) );
		
		assertEquals( 3, getSessions().getStatistics().getPrepareStatementCount() );
		
		r = (Parent) s.get( Parent.class, r.getName() );
		assertTrue( Hibernate.isInitialized( r.getChildren() ) );
		assertFalse( Hibernate.isInitialized( r.getMoreChildren() ) );
		assertEquals( r.getChildren().size(), 1 );
		assertEquals( r.getMoreChildren().size(), 0 );

		s.delete(p);
		s.delete(q);		
		s.delete(r);
		
		t.commit();
		s.close();
	}
	
	public void testManyToManyCriteriaJoin() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Parent p = new Parent("foo");
		p.getChildren().add( new Child("foo1") );
		p.getChildren().add( new Child("foo2") );
		Parent q = new Parent("bar");
		q.getChildren().add( new Child("bar1") );
		q.getChildren().add( new Child("bar2") );
		q.getMoreChildren().addAll( p.getChildren() );
		s.persist(p); 
		s.persist(q);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		
		List parents = s.createCriteria(Parent.class)
			.createCriteria("moreChildren")
			.createCriteria("friends")
			.addOrder( Order.desc("name") )
			.list();

		parents = s.createCriteria(Parent.class)
			.setFetchMode("moreChildren", FetchMode.JOIN)
			.setFetchMode("moreChildren.friends", FetchMode.JOIN)
			.addOrder( Order.desc("name") )
			.list();

		s.delete( parents.get(0) );
		s.delete( parents.get(1) );

		t.commit();
		s.close();
	}
	
	public void testSubselectFetchCriteria() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Parent p = new Parent("foo");
		p.getChildren().add( new Child("foo1") );
		p.getChildren().add( new Child("foo2") );
		Parent q = new Parent("bar");
		q.getChildren().add( new Child("bar1") );
		q.getChildren().add( new Child("bar2") );
		q.getMoreChildren().addAll( p.getChildren() );
		s.persist(p); 
		s.persist(q);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		
		getSessions().getStatistics().clear();
		
		List parents = s.createCriteria(Parent.class)
			.add( Property.forName("name").between("bar", "foo") )
			.addOrder( Order.desc("name") )
			.list();
		p = (Parent) parents.get(0);
		q = (Parent) parents.get(1);

		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		assertFalse( Hibernate.isInitialized( q.getChildren() ) );

		assertEquals( p.getChildren().size(), 2 );
				
		assertTrue( Hibernate.isInitialized( p.getChildren().iterator().next() ) );
		
		assertTrue( Hibernate.isInitialized( q.getChildren() ) );
		
		assertEquals( q.getChildren().size(), 2 );
		
		assertTrue( Hibernate.isInitialized( q.getChildren().iterator().next() ) );
		
		assertFalse( Hibernate.isInitialized( p.getMoreChildren() ) );
		assertFalse( Hibernate.isInitialized( q.getMoreChildren() ) );

		assertEquals( p.getMoreChildren().size(), 0 );
		
		assertTrue( Hibernate.isInitialized( q.getMoreChildren() ) );
	
		assertEquals( q.getMoreChildren().size(), 2 );
		
		assertTrue( Hibernate.isInitialized( q.getMoreChildren().iterator().next() ) );
		
		assertEquals( 3, getSessions().getStatistics().getPrepareStatementCount() );

		Child c = (Child) p.getChildren().get(0);
		c.getFriends().size();

		s.delete(p);
		s.delete(q);		

		t.commit();
		s.close();
	}

}

