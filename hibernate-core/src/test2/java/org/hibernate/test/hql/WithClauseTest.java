/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Implementation of WithClauseTest.
 *
 * @author Steve Ebersole
 */
public class WithClauseTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "hql/Animal.hbm.xml", "hql/SimpleEntityWithAssociation.hbm.xml" };
	}

	@Test
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
		catch (IllegalArgumentException e) {
			assertTyping( QueryException.class, e.getCause() );
		}
		catch ( HibernateException e ) {
			// the expected response...
		}

		txn.commit();
		s.close();

		data.cleanup();
	}

	@Test
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

		list = s.createQuery( "from Human h inner join h.friends f with f.bodyWeight < :someLimit" )
				.setDouble( "someLimit", 25 )
				.list();
		assertTrue( "ad-hoc on did take effect", !list.isEmpty() );

		// many-to-many
		list = s.createQuery( "from Human h inner join h.friends as f with f.nickName like 'bubba'" )
				.list();
		assertTrue( "ad-hoc on did not take effect", list.isEmpty() );

		// http://opensource.atlassian.com/projects/hibernate/browse/HHH-1930
		list = s.createQuery( "from Human h inner join h.nickNames as nicknames with nicknames = 'abc'" )
				.list();
		assertTrue( "ad-hoc on did not take effect", list.isEmpty() );

		list = s.createQuery( "from Human h inner join h.offspring o with o.mother.father = :cousin" )
				.setEntity( "cousin", s.load( Human.class, Long.valueOf( "123" ) ) )
				.list();
		assertTrue( "ad-hoc did take effect", list.isEmpty() );

		txn.commit();
		s.close();

		data.cleanup();
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-2772")
	public void testWithJoinRHS() {
		Session s = openSession();
		s.beginTransaction();

		SimpleEntityWithAssociation entity1 = new SimpleEntityWithAssociation();
		entity1.setName( "entity1" );
		SimpleEntityWithAssociation entity2 = new SimpleEntityWithAssociation();
		entity2.setName( "entity2" );
		
		SimpleAssociatedEntity associatedEntity1 = new SimpleAssociatedEntity();
		associatedEntity1.setName( "associatedEntity1" );
		SimpleAssociatedEntity associatedEntity2 = new SimpleAssociatedEntity();
		associatedEntity2.setName( "associatedEntity2" );
		
		entity1.addAssociation( associatedEntity1 );
		entity2.addAssociation( associatedEntity2 );
		
		s.persist( entity1 );
		s.persist( entity2 );
		
		s.getTransaction().commit();
		s.clear();
		
		s.beginTransaction();
		
		Query query = s.createQuery( "select a from SimpleEntityWithAssociation as e INNER JOIN e.associatedEntities as a WITH e.name=?1" );
		query.setParameter( 1, "entity1" );
		List list = query.list();
		assertEquals( list.size(), 1 );
		
		SimpleAssociatedEntity associatedEntity = (SimpleAssociatedEntity) query.list().get( 0 );
		assertNotNull( associatedEntity );
		assertEquals( associatedEntity.getName(), "associatedEntity1" );
		assertEquals( associatedEntity.getOwner().getName(), "entity1" );
		
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9329")
	public void testWithClauseAsSubquery() {
		TestData data = new TestData();
		data.prepare();

		Session s = openSession();
		Transaction txn = s.beginTransaction();

		// Since friends has a join table, we will first left join all friends and then do the WITH clause on the target entity table join
		// Normally this produces 2 results which is wrong and can only be circumvented by converting the join table and target entity table join to a subquery
		List list = s.createQuery( "from Human h left join h.friends as f with f.nickName like 'bubba' where h.description = 'father'" )
				.list();
		assertEquals( "subquery rewriting of join table did not take effect", 1, list.size() );

		txn.commit();
		s.close();

		data.cleanup();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11230")
	public void testWithClauseAsSubqueryWithEqualOperator() {
		TestData data = new TestData();
		data.prepare();

		Session s = openSession();
		Transaction txn = s.beginTransaction();

		// Like testWithClauseAsSubquery but uses equal operator since it render differently in SQL
		List list = s.createQuery( "from Human h left join h.friends as f with f.nickName = 'bubba' where h.description = 'father'" )
				.list();
		assertEquals( "subquery rewriting of join table did not take effect", 1, list.size() );

		txn.commit();
		s.close();

		data.cleanup();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9329")
	public void testWithClauseAsSubqueryWithKey() {
		TestData data = new TestData();
		data.prepare();

		Session s = openSession();
		Transaction txn = s.beginTransaction();

		// Since family has a join table, we will first left join all family members and then do the WITH clause on the target entity table join
		// Normally this produces 2 results which is wrong and can only be circumvented by converting the join table and target entity table join to a subquery
		List list = s.createQuery( "from Human h left join h.family as f with key(f) like 'son1' where h.description = 'father'" )
				.list();
		assertEquals( "subquery rewriting of join table did not take effect", 1, list.size() );

		txn.commit();
		s.close();

		data.cleanup();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11157")
	public void testWithClauseAsNonSubqueryWithKey() {
		rebuildSessionFactory( c -> c.setProperty( AvailableSettings.COLLECTION_JOIN_SUBQUERY, "false" ) );

		try {
			TestData data = new TestData();
			data.prepare();

			Session s = openSession();
			Transaction txn = s.beginTransaction();

			// Since family has a join table, we will first left join all family members and then do the WITH clause on the target entity table join
			// Normally this produces 2 results which is wrong and can only be circumvented by converting the join table and target entity table join to a subquery
			List list = s.createQuery( "from Human h left join h.family as f with key(f) like 'son1' where h.description = 'father'" )
					.list();
			assertEquals( "subquery rewriting of join table was not disabled", 2, list.size() );

			txn.commit();
			s.close();

			data.cleanup();
		} finally {
			// Rebuild to reset the properties
			rebuildSessionFactory();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11401")
	public void testWithClauseAsSubqueryWithKeyAndOtherJoinReference() {
		TestData data = new TestData();
		data.prepare();

		Session s = openSession();
		Transaction txn = s.beginTransaction();

		// Just a stupid example that makes use of a column that isn't from the collection table or the target entity table
		List list = s.createQuery( "from Human h join h.friends as friend left join h.family as f with key(f) = concat('son', cast(friend.intValue as string)) where h.description = 'father'" )
				.list();
		assertEquals( "subquery rewriting of join table did not take effect", 2, list.size() );

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
			friend.setIntValue( 1 );

			Human friend2 = new Human();
			friend2.setBodyWeight( 20 );
			friend2.setDescription( "friend2" );
			friend.setIntValue( 2 );

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
			father.getFriends().add( friend2 );

			session.save( mother );
			session.save( father );
			session.save( child1 );
			session.save( child2 );
			session.save( friend );
			session.save( friend2 );

			father.setFamily( new HashMap() );
			father.getFamily().put( "son1", child1 );
			father.getFamily().put( "son2", child2 );

			txn.commit();
			session.close();
		}

		public void cleanup() {
			Session session = openSession();
			Transaction txn = session.beginTransaction();
			Human father = (Human) session.createQuery( "from Human where description = 'father'" ).uniqueResult();
			father.getFriends().clear();
			father.getFamily().clear();
			session.flush();
			session.delete( session.createQuery( "from Human where description = 'friend2'" ).uniqueResult() );
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
