package org.hibernate.orm.test.query.hql;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.orm.test.hql.fetchAndJoin.Entity1;
import org.hibernate.orm.test.hql.fetchAndJoin.Entity2;
import org.hibernate.orm.test.hql.fetchAndJoin.Entity3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = {
				Entity1.class,
				Entity2.class,
				Entity3.class
		}
)
@SessionFactory(useCollectingStatementObserver = true)
public class ToOneFetchAndJoinTest {

	@Test
	@JiraKey("HHH-15109")
	public void testNestedCriteriaFetchWithImplicitPathRestrictions(SessionFactoryScope scope) {
		final var statementObserver = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Entity1.class );
			final var root = query.from( Entity1.class );
			root.fetch( "entity2" ).fetch( "entity3" );
			query.select( root ).where( builder.and(
					builder.like( root.get( "entity2" ).get( "value" ), "%entity2%" ),
					builder.like( root.get( "entity2" ).get( "entity3" ).get( "value" ), "%entity3%" )
			) );

			statementObserver.clear();
			final var results = session.createQuery( query ).getResultList();
			assertThat( results ).hasSize( 1 );
			final var entity = results.get( 0 );
			assertTrue( Hibernate.isInitialized( entity.getEntity2() ) );
			assertTrue( Hibernate.isInitialized( entity.getEntity2().getEntity3() ) );
			assertEquals( "entity1", entity.getValue() );
			assertEquals( "entity2", entity.getEntity2().getValue() );
			assertEquals( "entity3", entity.getEntity2().getEntity3().getValue() );

			assertThat( statementObserver.getSqlQueries() ).hasSize( 1 );
			assertThat( statementObserver.getSqlQueries().get( 0 ) )
					.containsOnlyOnce( " join entity2 " )
					.containsOnlyOnce( " join entity3 " )
					.doesNotContain( " cross join " );
		} );
	}

	@Test
	@JiraKey( value = "HHH-9637")
	public void testFetchJoinsWithImplicitJoinInRestriction(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final String qry =
							"select e1 " +
							"from Entity1 e1 " +
							"		inner join fetch e1.entity2 e2 " +
							"		inner join fetch e2.entity3 " +
							"where e1.entity2.value = 'entity2'";
					Entity1 e1Queryied = session.createQuery( qry, Entity1.class ).uniqueResult();
					assertEquals( "entity1", e1Queryied.getValue() );
					assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2() ) );
					assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2().getEntity3() ) );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-9637")
	public void testExplicitJoinBeforeFetchJoins(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final String qry =
							"select e1 " +
							"from Entity1 e1 " +
							"		inner join e1.entity2 e1Restrict " +
							"		inner join fetch e1.entity2 e2" +
							"		inner join fetch e2.entity3 " +
							"where e1Restrict.value = 'entity2'";
					Entity1 e1Queryied = session.createQuery( qry, Entity1.class ).uniqueResult();
					assertEquals( "entity1", e1Queryied.getValue() );
					assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2() ) );
					assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2().getEntity3() ) );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-9637")
	public void testExplicitJoinBetweenFetchJoins(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final String qry =
							"select e1 " +
							"from Entity1 e1 " +
							"		inner join fetch e1.entity2 e2 " +
							"		inner join e1.entity2 e1Restrict " +
							"		inner join fetch e2.entity3 " +
							"where e1Restrict.value = 'entity2'";
					Entity1 e1Queryied = session.createQuery( qry, Entity1.class ).uniqueResult();
					assertEquals( "entity1", e1Queryied.getValue() );
					assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2() ) );
					assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2().getEntity3() ) );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-9637")
	public void testExplicitJoinAfterFetchJoins(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final String qry =
							"select e1 " +
							"from Entity1 e1 " +
							"		inner join fetch e1.entity2 e2 " +
							"		inner join fetch e2.entity3 " +
							"		inner join e1.entity2 e1Restrict " +
							"where e1Restrict.value = 'entity2'";
					Entity1 e1Queryied = session.createQuery( qry, Entity1.class ).uniqueResult();
					assertEquals( "entity1", e1Queryied.getValue() );
					assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2() ) );
					assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2().getEntity3() ) );
				}
		);
	}

	@BeforeAll
	public void setupData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Entity1 e1 = new Entity1();
					e1.setValue( "entity1" );
					Entity2 e2 = new Entity2();
					e2.setValue( "entity2" );
					Entity3 e3 = new Entity3();
					e3.setValue( "entity3" );

					e1.setEntity2( e2 );
					e2.setEntity3( e3 );

					Entity2 e2a = new Entity2();
					e2a.setValue( "entity2a" );

					session.persist( e3 );
					session.persist( e2 );
					session.persist( e1 );
					session.persist( e2a );
				}
		);
	}

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		// scope.getSessionFactory().getSchemaManager().truncate();
	}
}
