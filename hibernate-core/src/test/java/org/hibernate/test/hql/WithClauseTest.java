// $Id: WithClauseTest.java 10945 2006-12-07 14:50:42Z steve.ebersole@jboss.com $
package org.hibernate.test.hql;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.hql.ast.InvalidWithClauseException;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * Implementation of WithClauseTest.
 *
 * @author Steve Ebersole
 */
public class WithClauseTest extends FunctionalTestCase {

	public WithClauseTest(String name) {
		super( name );
	}

	public String[] getMappings() {
		return new String[] { "hql/Animal.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( WithClauseTest.class );
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
	}

	public void testWithClauseFailsWithFetch() {
		TestData data = new TestData();
		data.prepare();

		Session s = openSession();
		Transaction txn = s.beginTransaction();

		try {
			s.createQuery( "from Animal a inner join fetch a.offspring as o with o.bodyWeight = :someLimit" )
			        .setDouble( "someLimit", 1 )
			        .list();
			fail( "ad-hoc on clause allowed with fetched association" );
		}
		catch ( HibernateException e ) {
			// the expected response...
		}

		txn.commit();
		s.close();

		data.cleanup();
	}

	public void testInvalidWithSemantics() {
		Session s = openSession();
		Transaction txn = s.beginTransaction();

		try {
			// PROBLEM : f.bodyWeight is a reference to a column on the Animal table; however, the 'f'
			// alias relates to the Human.friends collection which the aonther Human entity.  The issue
			// here is the way JoinSequence and Joinable (the persister) interact to generate the
			// joins relating to the sublcass/superclass tables
			s.createQuery( "from Human h inner join h.friends as f with f.bodyWeight < :someLimit" )
					.setDouble( "someLimit", 1 )
					.list();
			fail( "failure expected" );
		}
		catch( InvalidWithClauseException expected ) {
		}

		try {
			s.createQuery( "from Animal a inner join a.offspring o inner join o.mother as m inner join m.father as f with o.bodyWeight > 1" )
					.list();
			fail( "failure expected" );
		}
		catch( InvalidWithClauseException expected ) {
		}

		try {
			s.createQuery( "from Human h inner join h.offspring o with o.mother.father = :cousin" )
					.setEntity( "cousin", s.load( Human.class, new Long(123) ) )
					.list();
			fail( "failure expected" );
		}
		catch( InvalidWithClauseException expected ) {
		}

		txn.commit();
		s.close();
	}

	public void testWithClause() {
		TestData data = new TestData();
		data.prepare();

		Session s = openSession();
		Transaction txn = s.beginTransaction();

		// one-to-many
		List list = s.createQuery( "from Human h inner join h.offspring as o with o.bodyWeight < :someLimit" )
				.setDouble( "someLimit", 1 )
				.list();
		assertTrue( "ad-hoc on did not take effect", list.isEmpty() );

		// many-to-one
		list = s.createQuery( "from Animal a inner join a.mother as m with m.bodyWeight < :someLimit" )
				.setDouble( "someLimit", 1 )
				.list();
		assertTrue( "ad-hoc on did not take effect", list.isEmpty() );

		// many-to-many
		list = s.createQuery( "from Human h inner join h.friends as f with f.nickName like 'bubba'" )
				.list();
		assertTrue( "ad-hoc on did not take effect", list.isEmpty() );

		// http://opensource.atlassian.com/projects/hibernate/browse/HHH-1930
		list = s.createQuery( "from Human h inner join h.nickNames as nicknames with nicknames = 'abc'" )
				.list();
		assertTrue( "ad-hoc on did not take effect", list.isEmpty() );

		txn.commit();
		s.close();

		data.cleanup();
	}

	private class TestData {
		public void prepare() {
			Session session = openSession();
			Transaction txn = session.beginTransaction();

			Human mother = new Human();
			mother.setBodyWeight( 10 );
			mother.setDescription( "mother" );

			Human father = new Human();
			father.setBodyWeight( 15 );
			father.setDescription( "father" );

			Human child1 = new Human();
			child1.setBodyWeight( 5 );
			child1.setDescription( "child1" );

			Human child2 = new Human();
			child2.setBodyWeight( 6 );
			child2.setDescription( "child2" );

			Human friend = new Human();
			friend.setBodyWeight( 20 );
			friend.setDescription( "friend" );

			child1.setMother( mother );
			child1.setFather( father );
			mother.addOffspring( child1 );
			father.addOffspring( child1 );

			child2.setMother( mother );
			child2.setFather( father );
			mother.addOffspring( child2 );
			father.addOffspring( child2 );

			father.setFriends( new ArrayList() );
			father.getFriends().add( friend );

			session.save( mother );
			session.save( father );
			session.save( child1 );
			session.save( child2 );
			session.save( friend );

			txn.commit();
			session.close();
		}

		public void cleanup() {
			Session session = openSession();
			Transaction txn = session.beginTransaction();
			Human father = (Human) session.createQuery( "from Human where description = 'father'" ).uniqueResult();
			father.getFriends().clear();
			session.flush();
			session.delete( session.createQuery( "from Human where description = 'friend'" ).uniqueResult() );
			session.delete( session.createQuery( "from Human where description = 'child1'" ).uniqueResult() );
			session.delete( session.createQuery( "from Human where description = 'child2'" ).uniqueResult() );
			session.delete( session.createQuery( "from Human where description = 'mother'" ).uniqueResult() );
			session.delete( father );
			session.createQuery( "delete Animal" ).executeUpdate();
			txn.commit();
			session.close();
		}
	}
}
