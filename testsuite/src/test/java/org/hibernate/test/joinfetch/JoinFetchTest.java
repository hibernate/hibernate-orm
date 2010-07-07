//$Id: JoinFetchTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.joinfetch;

import java.util.List;

import junit.framework.Test;

import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class JoinFetchTest extends FunctionalTestCase {
	
	public JoinFetchTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "joinfetch/ItemBid.hbm.xml", "joinfetch/UserGroup.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		cfg.setProperty(Environment.MAX_FETCH_DEPTH, "10");
		cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "false");
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( JoinFetchTest.class );
	}
	
	public void testProjection() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.createCriteria(Item.class).setProjection( Projections.rowCount() ).uniqueResult();
		s.createCriteria(Item.class).uniqueResult();
		t.commit();
		s.close();
	}

	public void testJoinFetch() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.createQuery( "delete from Bid" ).executeUpdate();
		s.createQuery( "delete from Comment" ).executeUpdate();
		s.createQuery( "delete from Item" ).executeUpdate();
		t.commit();
		s.close();
		
		Category cat = new Category("Photography");
		Item i = new Item(cat, "Camera");
		Bid b = new Bid(i, 100.0f);
		new Bid(i, 105.0f);
		new Comment(i, "This looks like a really good deal");
		new Comment(i, "Is it the latest version?");
		new Comment(i, "<comment deleted>");
		System.out.println( b.getTimestamp() );
		
		s = openSession();
		t = s.beginTransaction();
		s.persist(cat);
		s.persist(i);
		t.commit();
		s.close();
		
		getSessions().evict(Item.class);

		s = openSession();
		t = s.beginTransaction();
		i = (Item) s.get( Item.class, i.getId() );
		assertTrue( Hibernate.isInitialized( i.getBids() ) );
		assertEquals( i.getBids().size(), 2 );
		assertTrue( Hibernate.isInitialized( i.getComments() ) );
		assertEquals( i.getComments().size(), 3 );
		t.commit();
		s.close();

		getSessions().evict(Bid.class);

		s = openSession();
		t = s.beginTransaction();
		b = (Bid) s.get( Bid.class, b.getId() );
		assertTrue( Hibernate.isInitialized( b.getItem() ) );
		assertTrue( Hibernate.isInitialized( b.getItem().getComments() ) );
		assertEquals( b.getItem().getComments().size(), 3 );
		System.out.println( b.getTimestamp() );
		t.commit();
		s.close();

		getSessions().evictCollection(Item.class.getName() + ".bids");
		
		s = openSession();
		t = s.beginTransaction();
		i = (Item) s.createCriteria( Item.class )
			.setFetchMode("bids", FetchMode.SELECT)
			.setFetchMode("comments", FetchMode.SELECT)
			.uniqueResult();
		assertFalse( Hibernate.isInitialized( i.getBids() ) );
		assertFalse( Hibernate.isInitialized( i.getComments() ) );
		b = (Bid) i.getBids().iterator().next();
		assertTrue( Hibernate.isInitialized( b.getItem() ) );
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		i = (Item) s.createQuery("from Item i left join fetch i.bids left join fetch i.comments").uniqueResult();
		assertTrue( Hibernate.isInitialized( i.getBids() ) );
		assertTrue( Hibernate.isInitialized( i.getComments() ) );
		assertEquals( i.getComments().size(), 3 );
		assertEquals( i.getBids().size(), 2 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Object[] row = (Object[]) s.getNamedQuery(Item.class.getName() + ".all").list().get(0);
		i = (Item) row[0];
		assertTrue( Hibernate.isInitialized( i.getBids() ) );
		assertTrue( Hibernate.isInitialized( i.getComments() ) );
		assertEquals( i.getComments().size(), 3 );
		assertEquals( i.getBids().size(), 2 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		i = (Item) s.createCriteria(Item.class).uniqueResult();
		assertTrue( Hibernate.isInitialized( i.getBids() ) );
		assertTrue( Hibernate.isInitialized( i.getComments() ) );
		assertEquals( i.getComments().size(), 3 );
		assertEquals( i.getBids().size(), 2 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List bids = s.createQuery("from Bid b left join fetch b.item i left join fetch i.category").list();
		Bid bid = (Bid) bids.get(0);
		assertTrue( Hibernate.isInitialized( bid.getItem() ) );
		assertTrue( Hibernate.isInitialized( bid.getItem().getCategory() ) );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List pairs = s.createQuery("from Item i left join i.bids b left join fetch i.category").list();
		Item item = (Item) ( (Object[]) pairs.get(0) )[0];
		assertFalse( Hibernate.isInitialized( item.getBids() ) );
		assertTrue( Hibernate.isInitialized( item.getCategory() ) );
		s.clear();
		pairs = s.createQuery("from Item i left join i.bids b left join i.category").list();
		item = (Item) ( (Object[]) pairs.get(0) )[0];
		assertFalse( Hibernate.isInitialized( item.getBids() ) );
		assertTrue( Hibernate.isInitialized( item.getCategory() ) );
		s.clear();
		pairs = s.createQuery("from Bid b left join b.item i left join fetch i.category").list();
		bid = (Bid) ( (Object[]) pairs.get(0) )[0];
		assertTrue( Hibernate.isInitialized( bid.getItem() ) );
		assertTrue( Hibernate.isInitialized( bid.getItem().getCategory() ) );
		s.clear();
		pairs = s.createQuery("from Bid b left join b.item i left join i.category").list();
		bid = (Bid) ( (Object[]) pairs.get(0) )[0];
		assertTrue( Hibernate.isInitialized( bid.getItem() ) );
		assertTrue( Hibernate.isInitialized( bid.getItem().getCategory() ) );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.createQuery( "delete from Bid" ).executeUpdate();
		s.createQuery( "delete from Comment" ).executeUpdate();
		s.createQuery( "delete from Item" ).executeUpdate();
		s.createQuery( "delete from Category" ).executeUpdate();
		t.commit();
		s.close();

	}
	
	public void testCollectionFilter() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Group hb = new Group("hibernate");
		User gavin = new User("gavin");
		User max = new User("max");
		hb.getUsers().put("gavin", gavin);
		hb.getUsers().put("max", max);
		gavin.getGroups().put("hibernate", hb);
		max.getGroups().put("hibernate", hb);
		s.persist(hb);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		hb = (Group) s.createCriteria(Group.class)
				.setFetchMode("users", FetchMode.SELECT)
				.add( Restrictions.idEq("hibernate") )
				.uniqueResult();
		assertFalse( Hibernate.isInitialized( hb.getUsers() ) );
		//gavin = (User) s.createFilter( hb.getUsers(), "where index(this) = 'gavin'" ).uniqueResult();
		Long size = (Long) s.createFilter( hb.getUsers(), "select count(*)" ).uniqueResult();
		assertEquals( new Long(2), size );
		assertFalse( Hibernate.isInitialized( hb.getUsers() ) );
		s.delete(hb);
		t.commit();
		s.close();
		
	}
	
	public void testJoinFetchManyToMany() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Group hb = new Group("hibernate");
		User gavin = new User("gavin");
		User max = new User("max");
		hb.getUsers().put("gavin", gavin);
		hb.getUsers().put("max", max);
		gavin.getGroups().put("hibernate", hb);
		max.getGroups().put("hibernate", hb);
		s.persist(hb);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		hb = (Group) s.get(Group.class, "hibernate");
		assertTrue( Hibernate.isInitialized( hb.getUsers() ) );
		gavin = (User) hb.getUsers().get("gavin");
		assertFalse( Hibernate.isInitialized( gavin.getGroups() ) );
		max = (User) s.get(User.class, "max");
		assertFalse( Hibernate.isInitialized( max.getGroups() ) );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		hb = (Group) s.createCriteria(Group.class)
			.setFetchMode("users", FetchMode.JOIN)
			.setFetchMode("users.groups", FetchMode.JOIN)
			.uniqueResult();
		assertTrue( Hibernate.isInitialized( hb.getUsers() ) );
		gavin = (User) hb.getUsers().get("gavin");
		assertTrue( Hibernate.isInitialized( gavin.getGroups() ) );
		max = (User) s.get(User.class, "max");
		assertTrue( Hibernate.isInitialized( max.getGroups() ) );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.delete(hb);
		t.commit();
		s.close();
	}

}

