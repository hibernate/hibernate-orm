/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.criteria;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.common.JoinType;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaCrossJoin;
import org.hibernate.query.criteria.JpaDerivedJoin;
import org.hibernate.query.criteria.JpaEntityJoin;
import org.hibernate.query.criteria.JpaJoin;
import org.hibernate.query.criteria.JpaPath;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSubQuery;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@Jpa( annotatedClasses = { CriteriaQualifiedJoinTest.Primary.class, CriteriaQualifiedJoinTest.Secondary.class } )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17116" )
public class CriteriaQualifiedJoinTest {
	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Secondary secondaryA = new Secondary( 1, "a" );
			final Secondary secondaryB = new Secondary( 2, "b" );
			ArrayList<Object> entities = new ArrayList<Object>( List.of(
					secondaryA,
					secondaryB,
					new Primary( 1, secondaryA ),
					new Primary( 2, secondaryB )
			) );
			entities.forEach( entityManager::persist );
		} );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "delete from Primary" ).executeUpdate();
			entityManager.createQuery( "delete from Secondary" ).executeUpdate();
		} );
	}

	@Test
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsSubqueryInOnClause.class )
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsOrderByInCorrelatedSubquery.class )
	public void testJoinLateral(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final HibernateCriteriaBuilder cb = entityManager.unwrap( Session.class ).getCriteriaBuilder();
			final JpaCriteriaQuery<Tuple> query = cb.createTupleQuery();
			final JpaRoot<Primary> root = query.from( Primary.class );
			final JpaSubQuery<Tuple> subquery = query.subquery( Tuple.class );
			final JpaRoot<Primary> correlatedRoot = subquery.correlate( root );
			final JpaJoin<Primary, Secondary> secondaryJoin = correlatedRoot.join( "secondary" );
			subquery.multiselect( secondaryJoin.get( "name" ).alias( "name" ) );
			final JpaDerivedJoin<Tuple> joinLateral = root.joinLateral( subquery );
			final JpaPath<Integer> id = root.get( "id" );
			final JpaPath<String> name = joinLateral.get( "name" );
			query.multiselect( id, name ).orderBy( cb.asc( id ), cb.asc( name ) );
			final List<Tuple> list = entityManager.createQuery( query ).getResultList();
			assertQueryResults( list, List.of( 1, 2 ), List.of( "a", "b" ) );
		} );
	}

	@Test
	public void testEntityJoin(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final HibernateCriteriaBuilder cb = entityManager.unwrap( Session.class ).getCriteriaBuilder();
			final JpaCriteriaQuery<Tuple> query = cb.createTupleQuery();
			final JpaRoot<Primary> root = query.from( Primary.class );
			final JpaEntityJoin<Primary,Secondary> entityJoin = root.join( Secondary.class, JoinType.INNER );
			final JpaPath<Integer> id = root.get( "id" );
			entityJoin.on( cb.equal( id, entityJoin.get( "id" ) ) );
			final JpaPath<String> name = entityJoin.get( "name" );
			query.multiselect( id, name ).orderBy( cb.asc( id ), cb.asc( name ) );
			final List<Tuple> list = entityManager.createQuery( query ).getResultList();
			assertQueryResults( list, List.of( 1, 2 ), List.of( "a", "b" ) );
		} );
	}

	@Test
	public void testCrossJoin(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final HibernateCriteriaBuilder cb = entityManager.unwrap( Session.class ).getCriteriaBuilder();
			final JpaCriteriaQuery<Tuple> query = cb.createTupleQuery();
			final JpaRoot<Primary> root = query.from( Primary.class );
			final JpaCrossJoin<Secondary> crossJoin = root.crossJoin( Secondary.class );
			final JpaPath<Integer> id = root.get( "id" );
			final JpaPath<String> name = crossJoin.get( "name" );
			query.multiselect( id, name ).orderBy( cb.asc( id ), cb.asc( name ) );
			final List<Tuple> list = entityManager.createQuery( query ).getResultList();
			assertQueryResults( list, List.of( 1, 1, 2, 2 ), List.of( "a", "b", "a", "b" ) );
		} );
	}

	private void assertQueryResults(List<Tuple> resultList, List<Integer> ids, List<String> names) {
		assertThat( resultList ).hasSize( ids.size() );
		for ( int i = 0; i < ids.size(); i++ ) {
			assertThat( resultList.get( i ).get( 0, Integer.class ) ).isEqualTo( ids.get( i ) );
			assertThat( resultList.get( i ).get( 1, String.class ) ).isEqualTo( names.get( i ) );
		}
	}

	@Entity( name = "Primary" )
	@Table( name = "t_primary" )
	public static class Primary implements Serializable {
		@Id
		private int id;

		@OneToOne
		@JoinColumn( name = "secondary_id" )
		private Secondary secondary;

		public Primary() {
		}

		public Primary(int id, Secondary secondary) {
			this.id = id;
			this.secondary = secondary;
		}

		public Secondary getSecondary() {
			return secondary;
		}
	}

	@Entity( name = "Secondary" )
	@Table( name = "t_secondary" )
	public static class Secondary implements Serializable {
		@Id
		private int id;

		private String name;

		public Secondary() {
		}

		public Secondary(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
