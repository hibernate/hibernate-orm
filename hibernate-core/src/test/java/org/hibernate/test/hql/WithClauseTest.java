/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.hql.internal.ast.InvalidWithClauseException;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

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
		catch ( HibernateException e ) {
			// the expected response...
		}

		txn.commit();
		s.close();

		data.cleanup();
	}

	@Test
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
		
		Query query = s.createQuery( "select a from SimpleEntityWithAssociation as e INNER JOIN e.associatedEntities as a WITH e.name=?" );
		query.setParameter( 0, "entity1" );
		List list = query.list();
		assertEquals( list.size(), 1 );
		
		SimpleAssociatedEntity associatedEntity = (SimpleAssociatedEntity) query.list().get( 0 );
		assertNotNull( associatedEntity );
		assertEquals( associatedEntity.getName(), "associatedEntity1" );
		assertEquals( associatedEntity.getOwner().getName(), "entity1" );
		
		s.getTransaction().commit();
		s.close();
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
