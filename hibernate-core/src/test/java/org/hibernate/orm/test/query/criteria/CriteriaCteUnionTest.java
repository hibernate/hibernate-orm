/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.criteria;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaQuery;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = BasicEntity.class )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17508" )
public class CriteriaCteUnionTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new BasicEntity( 1, "entity_1" ) );
			session.persist( new BasicEntity( 2, "entity_2" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from BasicEntity" ).executeUpdate() );
	}

	@Test
	public void testSimpleCte(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Tuple> query = createQuery( cb, true, "entity_1" );
			final JpaCriteriaQuery<Tuple> mainQuery = cb.createTupleQuery();

			final JpaCteCriteria<Tuple> cteQuery = mainQuery.with( query );
			final JpaRoot<Tuple> mainRoot = mainQuery.from( cteQuery );
			mainQuery.multiselect(
					mainRoot.get( "id" ).alias( "id" ),
					mainRoot.get( "isMatch" ).alias( "isMatch" )
			);
			final Tuple criteriaResult = session.createQuery( mainQuery ).getSingleResult();

			final Tuple hqlResult = session.createQuery(
					"with cte as (" + createHql( true, "entity_1" ) + ")" +
							"select id, isMatch from cte",
					Tuple.class
			).getSingleResult();

			assertThat( hqlResult.get( 0, Integer.class ) )
					.isEqualTo( criteriaResult.get( 0, Integer.class ) )
					.isEqualTo( 1 );
			assertThat( hqlResult.get( 1, Boolean.class ) )
					.isEqualTo( criteriaResult.get( 1, Boolean.class ) )
					.isTrue();
		} );
	}

	@Test
	public void testUnionCte(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();

			final JpaCriteriaQuery<Tuple> query1 = createQuery( cb, true, "entity_1" );
			final JpaCriteriaQuery<Tuple> query2 = createQuery( cb, false, "entity_2" );
			final CriteriaQuery<Tuple> unionQuery = cb.unionAll( query1, query2 );

			final JpaCriteriaQuery<Tuple> mainQuery = cb.createTupleQuery();
			final JpaCteCriteria<Tuple> cteQuery = mainQuery.with( unionQuery );
			final JpaRoot<Tuple> mainRoot = mainQuery.from( cteQuery );
			mainQuery.multiselect(
					mainRoot.get( "id" ).alias( "id" ),
					mainRoot.get( "isMatch" ).alias( "isMatch" )
			);
			final List<Tuple> criteriaList = session.createQuery( mainQuery ).getResultList();

			final List<Tuple> hqlList = session.createQuery(
					"with cte as (" +
							createHql( true, "entity_1" ) +
							" union all " +
							createHql( false, "entity_2" ) + ") " +
							"select id, isMatch from cte",
					Tuple.class
			).getResultList();

			assertThat( hqlList.size() ).isEqualTo( criteriaList.size() ).isEqualTo( 2 );
			final Consumer<Tuple> assertResult = t -> assertThat( t.get( 1, Boolean.class ) ).isEqualTo( t.get( 0, Integer.class ) == 1 );
			hqlList.forEach( assertResult );
			criteriaList.forEach( assertResult );
		} );
	}

	private String createHql(boolean match, String value) {
		return String.format( "select id as id, %s as isMatch from BasicEntity e where e.data = '%s'", match, value );
	}

	private JpaCriteriaQuery<Tuple> createQuery(HibernateCriteriaBuilder cb, boolean match, String value) {
		final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
		final JpaRoot<BasicEntity> root = cq.from( BasicEntity.class );
		return cq.multiselect(
				root.get( "id" ).alias( "id" ),
				cb.literal( match ).alias( "isMatch" )
		).where(
				cb.equal( root.get( "data" ), value )
		);
	}
}
