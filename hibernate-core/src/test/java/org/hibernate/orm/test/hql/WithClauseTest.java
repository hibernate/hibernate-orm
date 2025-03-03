/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.QueryException;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Implementation of WithClauseTest.
 *
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/hql/Animal.hbm.xml",
				"org/hibernate/orm/test/hql/SimpleEntityWithAssociation.hbm.xml",
		}
)
@SessionFactory
public class WithClauseTest {
	private final TestData data = new TestData();

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		data.prepare( scope );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		data.cleanup( scope );
	}

	@Test
	public void testWithClauseFailsWithFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					try {
						session.createQuery( "from Animal a inner join fetch a.offspring as o with o.bodyWeight = :someLimit" )
								.setParameter( "someLimit", 1 )
								.list();
						fail( "ad-hoc on clause allowed with fetched association" );
					}
					catch (IllegalArgumentException e) {
						assertTyping( QueryException.class, e.getCause() );
					}
					catch ( HibernateException e ) {
						// the expected response...
					}
				}
		);
	}

	@Test
	public void testWithClause(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					// one-to-many
					List list = session.createQuery( "from Human h inner join h.offspring as o with o.bodyWeight < :someLimit", Human.class )
							.setParameter( "someLimit", 1 )
							.list();
					assertTrue( list.isEmpty(), "ad-hoc on did not take effect" );

					// many-to-one
					list = session.createQuery( "from Animal a inner join a.mother as m with m.bodyWeight < :someLimit", Animal.class )
							.setParameter( "someLimit", 1 )
							.list();
					assertTrue( list.isEmpty(), "ad-hoc on did not take effect" );

					list = session.createQuery( "from Human h inner join h.friends f with f.bodyWeight < :someLimit", Human.class )
							.setParameter( "someLimit", 25 )
							.list();
					assertTrue( !list.isEmpty(), "ad-hoc on did take effect" );

					// many-to-many
					list = session.createQuery( "from Human h inner join h.friends as f with f.nickName like 'bubba'", Human.class )
							.list();
					assertTrue( list.isEmpty(), "ad-hoc on did not take effect" );

					// http://opensource.atlassian.com/projects/hibernate/browse/HHH-1930
					list = session.createQuery( "from Human h inner join h.nickNames as nicknames with nicknames = 'abc'", Human.class )
							.list();
					assertTrue( list.isEmpty(), "ad-hoc on did not take effect" );

				}
		);
	}

	@Test
	public void testWithClauseWithImplicitJoin(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					List list = session.createQuery( "from Human h inner join h.offspring o with o.mother.father = :cousin", Object[].class )
							.setParameter( "cousin", session.getReference( Human.class, Long.valueOf( "123" ) ) )
							.list();
					assertTrue( list.isEmpty(), "ad-hoc did take effect" );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-2772")
	public void testWithJoinRHS(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
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

					session.persist( entity1 );
					session.persist( entity2 );
				}
		);

		scope.inTransaction(
				(session) -> {
					Query query = session.createQuery( "select a from SimpleEntityWithAssociation as e INNER JOIN e.associatedEntities as a WITH e.name=?1" );
					query.setParameter( 1, "entity1" );
					List list = query.list();
					assertEquals( list.size(), 1 );

					SimpleAssociatedEntity associatedEntity = (SimpleAssociatedEntity) query.list().get( 0 );
					assertNotNull( associatedEntity );
					assertEquals( associatedEntity.getName(), "associatedEntity1" );
					assertEquals( associatedEntity.getOwner().getName(), "entity1" );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-9329")
	public void testWithClauseAsSubquery(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					// Since friends has a join table, we will first left join all friends and then do the WITH clause on the target entity table join
					// Normally this produces 2 results which is wrong and can only be circumvented by converting the join table and target entity table join to a subquery
					List list = session.createQuery( "from Human h left join h.friends as f with f.nickName like 'bubba' where h.description = 'father'", Object[].class )
							.list();
					assertEquals( 1, list.size(), "subquery rewriting of join table did not take effect" );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-11230")
	public void testWithClauseAsSubqueryWithEqualOperator(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					// Like testWithClauseAsSubquery but uses equal operator since it render differently in SQL
					List list = session.createQuery( "from Human h left join h.friends as f with f.nickName = 'bubba' where h.description = 'father'", Object[].class )
							.list();
					assertEquals( 1, list.size(), "subquery rewriting of join table did not take effect" );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-9329")
	public void testWithClauseAsSubqueryWithKey(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					// Since family has a join table, we will first left join all family members and then do the WITH clause on the target entity table join
					// Normally this produces 2 results which is wrong and can only be circumvented by converting the join table and target entity table join to a subquery
					List list = session.createQuery( "from Human h left join h.family as f with key(f) like 'son1' where h.description = 'father'", Object[].class )
							.list();
					assertEquals( 1, list.size(), "subquery rewriting of join table did not take effect" );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-11401")
	@SkipForDialect(dialectClass = DerbyDialect.class, reason = "Derby does not support cast from INTEGER to VARCHAR")
	public void testWithClauseAsSubqueryWithKeyAndOtherJoinReference(SessionFactoryScope scope) {
		scope.inTransaction(
				(s) -> {
					// Just a stupid example that makes use of a column that isn't from the collection table or the target entity table
					List list = s.createQuery( "select 1 from Human h join h.friends as friend left join h.family as f with key(f) = concat('son', cast(friend.intValue as string)) where h.description = 'father'" )
							.list();
					assertEquals( 2, list.size(), "subquery rewriting of join table did not take effect" );
				}
		);
	}

	static class TestData {
		public void prepare(SessionFactoryScope scope) {
			scope.inTransaction(
					(session) -> {
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

						session.persist( mother );
						session.persist( father );
						session.persist( child1 );
						session.persist( child2 );
						session.persist( friend );
						session.persist( friend2 );

						father.setFamily( new HashMap() );
						father.getFamily().put( "son1", child1 );
						father.getFamily().put( "son2", child2 );
					}
			);
		}

		public void cleanup(SessionFactoryScope scope) {
			scope.inTransaction(
					(session) -> {
						Human father = (Human) session.createQuery( "from Human where description = 'father'" ).uniqueResult();
						if ( father != null ) {
							father.getFriends().clear();
							father.getFamily().clear();
							session.flush();
						}
						session.remove( session.createQuery( "from Human where description = 'friend2'" ).uniqueResult() );
						session.remove( session.createQuery( "from Human where description = 'friend'" ).uniqueResult() );
						session.remove( session.createQuery( "from Human where description = 'child1'" ).uniqueResult() );
						session.remove( session.createQuery( "from Human where description = 'child2'" ).uniqueResult() );
						session.remove( session.createQuery( "from Human where description = 'mother'" ).uniqueResult() );
						session.remove( father );
						session.createQuery( "delete Animal" ).executeUpdate();
						session.createQuery( "delete SimpleAssociatedEntity" ).executeUpdate();
						session.createQuery( "delete SimpleEntityWithAssociation" ).executeUpdate();
					}
			);
		}
	}
}
