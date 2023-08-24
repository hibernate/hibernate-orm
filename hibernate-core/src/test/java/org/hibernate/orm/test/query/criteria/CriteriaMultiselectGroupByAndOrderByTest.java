/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.criteria;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		CriteriaMultiselectGroupByAndOrderByTest.Primary.class,
		CriteriaMultiselectGroupByAndOrderByTest.Secondary.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17085" )
public class CriteriaMultiselectGroupByAndOrderByTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Secondary secondaryA = new Secondary( 1, "a" );
			final Secondary secondaryB = new Secondary( 2, "b" );
			final Secondary secondaryC = new Secondary( 3, "c" );
			final ArrayList<Object> entities = new ArrayList<>( List.of(
					new Primary( 1, new BigDecimal( "10" ), secondaryA ),
					new Primary( 2, new BigDecimal( "20" ), secondaryA ),
					new Primary( 3, new BigDecimal( "30" ), secondaryA ),
					new Primary( 4, new BigDecimal( "100" ), secondaryB ),
					new Primary( 5, new BigDecimal( "200" ), secondaryB ),
					new Primary( 6, new BigDecimal( "300" ), secondaryB ),
					new Primary( 7, new BigDecimal( "1000" ), secondaryC ),
					new Primary( 8, new BigDecimal( "2000" ), secondaryC ),
					new Primary( 9, new BigDecimal( "3000" ), secondaryC )
			) );
			entities.addAll( List.of( secondaryA, secondaryB, secondaryC ) );
			entities.forEach( session::persist );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Primary" ).executeUpdate();
			session.createMutationQuery( "delete from Secondary" ).executeUpdate();
		} );
	}

	@Test
	public void testCriteriaGroupBy(SessionFactoryScope scope) {
		executeQuery( scope, false );
	}

	@Test
	public void testCriteriaGroupByAndOrderBy(SessionFactoryScope scope) {
		executeQuery( scope, true );
	}

	private void executeQuery(SessionFactoryScope scope, boolean order) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Tuple> query = cb.createQuery( Tuple.class );
			final Root<Primary> root = query.from( Primary.class );
			final Join<Primary, Secondary> join = root.join( "secondary" );
			query.multiselect(
					join.get( "entityName" ).alias( "secondary" ),
					cb.sum( root.get( "amount" ) ).alias( "sum" )
			).groupBy( join );
			if ( order ) {
				query.orderBy( cb.desc( join.get( "entityName" ) ) );
			}
			final List<Tuple> resultList = session.createQuery( query ).getResultList();
			assertThat( resultList ).hasSize( 3 );
		} );
	}

	@Entity( name = "Primary" )
	@Table( name = "t_primary" )
	public static class Primary {
		@Id
		private int id;

		private BigDecimal amount;

		@ManyToOne( fetch = FetchType.LAZY )
		private Secondary secondary;

		public Primary() {
		}

		public Primary(int id, BigDecimal amount, Secondary secondary) {
			this.id = id;
			this.amount = amount;
			this.secondary = secondary;
		}

		public Secondary getSecondary() {
			return secondary;
		}

		public BigDecimal getAmount() {
			return amount;
		}
	}

	@Entity( name = "Secondary" )
	@Table( name = "t_secondary" )
	public static class Secondary {
		@Id
		private int id;

		private String entityName;

		public Secondary() {
		}

		public Secondary(int id, String entityName) {
			this.id = id;
			this.entityName = entityName;
		}

		public String getEntityName() {
			return entityName;
		}
	}
}
