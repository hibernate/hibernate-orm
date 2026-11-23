package org.hibernate.orm.test.jpa.criteria.query;

import java.util.List;

import org.hibernate.query.Query;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel(annotatedClasses = BasicEntity.class)
@JiraKey("HHH-16109")
public class NamedQueryTest {
	@BeforeAll
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new BasicEntity( 1, "test_1" ) );
			session.persist( new BasicEntity( 2, "test_2" ) );
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<BasicEntity> criteria = cb.createQuery( BasicEntity.class );
			criteria.select( criteria.from( BasicEntity.class ) );
			// Criteria
			final TypedQuery<BasicEntity> criteriaQuery = session.createQuery( criteria );
			scope.getSessionFactory().addNamedQuery( "criteria_query", criteriaQuery );
			// Criteria + limit / offset
			final TypedQuery<BasicEntity> criteriaQueryLimit = session.createQuery( criteria );
			criteriaQueryLimit.setFirstResult( 1 ).setMaxResults( 1 );
			scope.getSessionFactory().addNamedQuery( "criteria_query_limit", criteriaQueryLimit );
			// HQL
			final TypedQuery<BasicEntity> hqlQuery = session.createQuery( "from BasicEntity", BasicEntity.class );
			scope.getSessionFactory().addNamedQuery( "hql_query", hqlQuery );
			// HQL + limit / offset
			final TypedQuery<BasicEntity> hqlQueryLimit = session.createQuery( "from BasicEntity", BasicEntity.class );
			hqlQueryLimit.setFirstResult( 1 ).setMaxResults( 1 );
			scope.getSessionFactory().addNamedQuery( "hql_query_limit", hqlQueryLimit );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from BasicEntity" ).executeUpdate() );
	}

	@Test
	public void testCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Query<BasicEntity> query = session.createNamedQuery( "criteria_query", BasicEntity.class );
			assertNull( query.getQueryOptions().getLimit().getFirstRow() );
			assertNull( query.getQueryOptions().getLimit().getMaxRows() );
			assertEquals( 2, query.getResultList().size() );
		} );
	}

	@Test
	@JiraKey("HHH-15321")
	public void testCriteriaTupleAliases(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Tuple> criteria = cb.createTupleQuery();
			final Root<BasicEntity> root = criteria.from( BasicEntity.class );
			criteria.multiselect(
					root.get( "id" ).alias( "entityId" ),
					root.get( "data" ).alias( "entityData" )
			);
			criteria.orderBy( cb.asc( root.get( "id" ) ) );
			scope.getSessionFactory().addNamedQuery( "criteria_tuple_query", session.createQuery( criteria ) );
		} );

		scope.inTransaction( session -> {
			final List<Tuple> results = session.createNamedQuery( "criteria_tuple_query", Tuple.class ).getResultList();
			assertEquals( 2, results.size() );
			for ( int i = 0; i < results.size(); i++ ) {
				final Tuple tuple = results.get( i );
				assertEquals( i + 1, tuple.get( "entityId", Integer.class ) );
				assertEquals( "test_" + ( i + 1 ), tuple.get( "entityData", String.class ) );
				assertEquals( tuple.get( 0 ), tuple.get( "entityId" ) );
				assertEquals( tuple.get( 1 ), tuple.get( "entityData" ) );
				assertEquals( 2, tuple.getElements().size() );
				assertEquals( "entityId", tuple.getElements().get( 0 ).getAlias() );
				assertEquals( "entityData", tuple.getElements().get( 1 ).getAlias() );
				assertEquals( Integer.class, tuple.getElements().get( 0 ).getJavaType() );
				assertEquals( String.class, tuple.getElements().get( 1 ).getJavaType() );
			}
		} );
	}

	@Test
	public void testCriteriaLimit(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Query<BasicEntity> query = session.createNamedQuery( "criteria_query_limit", BasicEntity.class );
			assertEquals( 1, query.getQueryOptions().getLimit().getFirstRow() );
			assertEquals( 1, query.getQueryOptions().getLimit().getMaxRows() );
			assertEquals( 1, query.getResultList().size() );
		} );
	}

	@Test
	public void testHql(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Query<BasicEntity> query = session.createNamedQuery( "hql_query", BasicEntity.class );
			assertNull( query.getQueryOptions().getLimit().getFirstRow() );
			assertNull( query.getQueryOptions().getLimit().getMaxRows() );
			assertEquals( 2, query.getResultList().size() );
		} );
	}

	@Test
	public void testHqlLimit(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Query<BasicEntity> query = session.createNamedQuery( "hql_query_limit", BasicEntity.class );
			assertEquals( 1, query.getQueryOptions().getLimit().getFirstRow() );
			assertEquals( 1, query.getQueryOptions().getLimit().getMaxRows() );
			assertEquals( 1, query.getResultList().size() );
		} );
	}
}
