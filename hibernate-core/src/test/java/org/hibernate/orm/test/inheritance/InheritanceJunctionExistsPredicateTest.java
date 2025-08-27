/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		InheritanceJunctionExistsPredicateTest.AbstractEntity.class,
		InheritanceJunctionExistsPredicateTest.EntityA.class,
		InheritanceJunctionExistsPredicateTest.EntityB.class,
		InheritanceJunctionExistsPredicateTest.EntityAContainer.class,
		InheritanceJunctionExistsPredicateTest.EntityBContainer.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
@Jira( "https://hibernate.atlassian.net/browse/HHH-18174" )
public class InheritanceJunctionExistsPredicateTest {
	@Test
	public void testExistsDisjunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select c from EntityAContainer c "
						+ "where exists (select 1 from c.entities e where e.identifier like 'child%') "
						+ "or exists (select 1 from c.entities e)",
				EntityAContainer.class
		).getResultList() ).hasSize( 0 ) );
	}

	@Test
	public void testExistsConjunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select c from EntityBContainer c "
						+ "where exists (select 1 from c.entities e where e.identifier like 'child%') "
						+ "and exists (select 1 from c.entities e)",
				EntityBContainer.class
		).getResultList() ).hasSize( 1 ) );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityB entityB = new EntityB();
			entityB.setIdentifier( "child_b" );
			session.persist( entityB );
			final EntityAContainer containerA = new EntityAContainer();
			containerA.id = 1;
			session.persist( containerA );
			final EntityBContainer containerB = new EntityBContainer();
			containerB.id = 1;
			containerB.entities.add( entityB );
			session.persist( containerB );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from AbstractEntity" ).executeUpdate();
			session.createMutationQuery( "delete from EntityAContainer" ).executeUpdate();
			session.createMutationQuery( "delete from EntityBContainer" ).executeUpdate();
		} );
	}

	@Entity( name = "AbstractEntity" )
	@DiscriminatorColumn( name = "disc_col", discriminatorType = DiscriminatorType.INTEGER )
	static abstract class AbstractEntity {
		@Id
		@GeneratedValue
		private Integer id;

		private String identifier;

		public String getIdentifier() {
			return identifier;
		}

		public void setIdentifier(String identifier) {
			this.identifier = identifier;
		}
	}

	@Entity( name = "EntityA" )
	@DiscriminatorValue( "1" )
	static class EntityA extends AbstractEntity {
	}

	@Entity( name = "EntityB" )
	@DiscriminatorValue( "2" )
	static class EntityB extends AbstractEntity {
	}

	@Entity( name = "EntityAContainer" )
	@Table( name = "a_container" )
	static class EntityAContainer {
		@Id
		private Integer id;

		@OneToMany
		@JoinColumn( name = "reference" )
		private List<EntityA> entities = new ArrayList<>();
	}

	@Entity( name = "EntityBContainer" )
	@Table( name = "b_container" )
	static class EntityBContainer {
		@Id
		private Integer id;

		@OneToMany
		@JoinColumn( name = "reference" )
		private List<EntityB> entities = new ArrayList<>();
	}
}
