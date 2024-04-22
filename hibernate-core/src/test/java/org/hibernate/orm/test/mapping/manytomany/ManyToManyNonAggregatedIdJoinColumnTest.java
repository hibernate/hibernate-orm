/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.manytomany;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		ManyToManyNonAggregatedIdJoinColumnTest.EntityA.class,
		ManyToManyNonAggregatedIdJoinColumnTest.EntityB.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17925" )
public class ManyToManyNonAggregatedIdJoinColumnTest {
	@Test
	public void testMapping(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final EntityA entityA = session.createQuery( "from EntityA where id = 1", EntityA.class ).getSingleResult();
			assertThat( entityA.getEntityBList() ).hasSize( 2 )
					.extracting( EntityB::getName )
					.containsOnly( "entityb_1", "entityb_2" );
			assertThat( session.createQuery( "from EntityA where id = 2", EntityA.class ).getSingleResult().getEntityBList() ).isEmpty();
		} );
	}

	@Test
	public void testQueryFunctions(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final EntityA entityA = session.createQuery(
					"from EntityA e join fetch e.entityBList where e.id = 1",
					EntityA.class
			).getSingleResult();
			assertThat( entityA.getEntityBList() ).matches(
							Hibernate::isInitialized,
							"Expected join-fetched list to be initialized"
					)
					.hasSize( 2 )
					.extracting( EntityB::getName )
					.containsOnly( "entityb_1", "entityb_2" );
		} );
		scope.inSession( session -> {
			assertThat( session.createQuery(
					"select size(e.entityBList) from EntityA e where e.id = 2",
					Integer.class
			).getSingleResult() ).isEqualTo( 0 );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA entityA = new EntityA( 1L, "entitya_1" );
			entityA.getEntityBList().add( new EntityB( 1L, "entityb_1" ) );
			entityA.getEntityBList().add( new EntityB( 2L, "entityb_2" ) );
			session.persist( entityA );
			session.persist( new EntityA( 2L, "entitya_2" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from EntityA" ).executeUpdate();
			session.createMutationQuery( "delete from EntityB" ).executeUpdate();
		} );
	}

	@Entity( name = "EntityA" )
	static class EntityA {
		@Id
		private Long id;

		@Id
		private String name;

		@ManyToMany( cascade = CascadeType.ALL )
		@JoinTable(
				name = "a_b_table",
				joinColumns = @JoinColumn( name = "entitya_id", referencedColumnName = "id" ),
				inverseJoinColumns = @JoinColumn( name = "entityb_id", referencedColumnName = "id" ) )
		private List<EntityB> entityBList = new ArrayList<>();

		public EntityA() {
		}

		public EntityA(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public List<EntityB> getEntityBList() {
			return entityBList;
		}
	}

	@Entity( name = "EntityB" )
	static class EntityB {
		@Id
		private Long id;

		private String name;

		public EntityB() {
		}

		public EntityB(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
