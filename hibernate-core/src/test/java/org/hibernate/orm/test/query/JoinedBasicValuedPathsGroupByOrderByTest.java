/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Beikov
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		JoinedBasicValuedPathsGroupByOrderByTest.EntityA.class
} )
@Jira( "https://hibernate.atlassian.net/issues/HHH-18272" )
public class JoinedBasicValuedPathsGroupByOrderByTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityA e1 = new EntityA( 1L, null, 1 );
			session.persist( e1 );
			session.persist( new EntityA( 2L, e1, 2 ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from EntityA" ).executeUpdate();
		} );
	}

	@Test
	public void testOrderByAlias(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery(
					"select " +
							"a.id aid, " +
							"p.id pid " +
							"from EntityA a " +
							"join a.parent p " +
							"order by aid",
					Object[].class
			).getResultList();
		} );
	}

	@Entity( name = "EntityA" )
	public static class EntityA {
		@Id
		private Long id;
		@ManyToOne(fetch = FetchType.LAZY)
		private EntityA parent;
		private Integer amount;

		public EntityA() {
		}

		public EntityA(Long id, EntityA parent, Integer amount) {
			this.id = id;
			this.parent = parent;
			this.amount = amount;
		}

		public Long getId() {
			return id;
		}

		public EntityA getParent() {
			return parent;
		}

		public Integer getAmount() {
			return amount;
		}

	}
}
