/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.criteria.subquery;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaEntityJoin;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSubQuery;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@Jpa( annotatedClasses = {
		SubqueryCorrelatedEntityJoinTest.Primary.class,
		SubqueryCorrelatedEntityJoinTest.Secondary.class,
		SubqueryCorrelatedEntityJoinTest.Tertiary.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17407" )
public class SubqueryCorrelatedEntityJoinTest {
	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> List.of(
				new Primary( 1, 10 ),
				new Secondary( 10, "n10" ),
				new Tertiary( 100, 10, "n100" )
		).forEach( em::persist ) );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			em.createQuery( "delete from PrimaryEntity" ).executeUpdate();
			em.createQuery( "delete from SecondaryEntity" ).executeUpdate();
			em.createQuery( "delete from TertiaryEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testCorrelatedEntityJoin(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			final HibernateCriteriaBuilder cb = em.unwrap( Session.class ).getCriteriaBuilder();
			final JpaCriteriaQuery<Tuple> query = cb.createTupleQuery();
			final JpaRoot<Primary> primary = query.from( Primary.class );
			final JpaEntityJoin<Secondary> secondaryJoin = primary.join( Secondary.class );
			secondaryJoin.on(
					cb.equal( primary.get( "secondaryFk" ), secondaryJoin.get( "id" ) )
			);
			final JpaSubQuery<String> subquery = query.subquery( String.class );
			final JpaRoot<Tertiary> tertiary = subquery.from( Tertiary.class );
			final JpaEntityJoin<Secondary> correlatedSecondaryJoin = subquery.correlate( secondaryJoin );
			subquery.select( tertiary.get( "name" ) ).where( cb.equal(
					tertiary.get( "secondaryFk" ),
					correlatedSecondaryJoin.get( "id" )
			) );
			query.multiselect( primary.get( "id" ), secondaryJoin.get( "name" ), subquery );
			final Tuple result = em.createQuery( query ).getSingleResult();
			assertThat( result.get( 0, Integer.class ) ).isEqualTo( 1 );
			assertThat( result.get( 1, String.class ) ).isEqualTo( "n10" );
			assertThat( result.get( 2, String.class ) ).isEqualTo( "n100" );
		} );
	}

	@Entity( name = "PrimaryEntity" )
	public static class Primary implements Serializable {
		@Id
		private Integer id;

		private Integer secondaryFk;

		public Primary() {
		}

		public Primary(Integer id, Integer secondaryFk) {
			this.id = id;
			this.secondaryFk = secondaryFk;
		}
	}

	@Entity( name = "SecondaryEntity" )
	public static class Secondary implements Serializable {
		@Id
		private Integer id;

		private String name;

		public Secondary() {
		}

		public Secondary(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity( name = "TertiaryEntity" )
	public static class Tertiary implements Serializable {
		@Id
		private Integer id;

		private Integer secondaryFk;

		private String name;

		public Tertiary() {
		}

		public Tertiary(Integer id, Integer secondaryFk, String name) {
			this.id = id;
			this.secondaryFk = secondaryFk;
			this.name = name;
		}
	}
}
