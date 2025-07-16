/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.criteria.subquery;

import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.query.criteria.*;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

@Jpa( annotatedClasses = {
		AttributeJoinOnCorrelatedOrderedJoinTest.Primary.class,
		AttributeJoinOnCorrelatedOrderedJoinTest.Secondary.class,
		AttributeJoinOnCorrelatedOrderedJoinTest.Tertiary.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-19550" )
public class AttributeJoinOnCorrelatedOrderedJoinTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			final HibernateCriteriaBuilder cb = em.unwrap( Session.class ).getCriteriaBuilder();
			final JpaCriteriaQuery<Tuple> query = cb.createTupleQuery();
			final JpaRoot<Primary> primary = query.from( Primary.class );
			// setup ordered joins
			final JpaEntityJoin<Primary> entityJoinedPrimary = primary.join( Primary.class );
			entityJoinedPrimary.on( primary.get( "id" ).equalTo( entityJoinedPrimary.get( "id" ) ) );
			// Need an attribute join to correlate
			final JpaJoin<?, Secondary> secondaryJoin = primary.join( "secondary" );

			final JpaSubQuery<String> subquery = query.subquery( String.class );
			final JpaJoin<?, Secondary> correlatedSecondaryJoin = subquery.correlate( secondaryJoin );
			// The association join is being added to the result of getLhs().findRoot()
			// so if the correlated join returns a wrong node, this is messed up
			// and will produce an exception when copying the criteria tree
			final JpaJoin<?, Tertiary> tertiary = correlatedSecondaryJoin.join( "tertiary" );
			subquery.select( tertiary.get( "name" ) ).where( cb.equal(
					tertiary.get( "secondaryFk" ),
					correlatedSecondaryJoin.get( "id" )
			) );
			query.multiselect( primary.get( "id" ), secondaryJoin.get( "name" ), subquery );
			em.createQuery( query ).getResultList();
		} );
	}

	@Entity( name = "PrimaryEntity" )
	public static class Primary implements Serializable {
		@Id
		private Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Secondary secondary;

		public Primary() {
		}
	}

	@Entity( name = "SecondaryEntity" )
	public static class Secondary implements Serializable {
		@Id
		private Integer id;

		private String name;
		@ManyToOne(fetch = FetchType.LAZY)
		private Tertiary tertiary;

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
