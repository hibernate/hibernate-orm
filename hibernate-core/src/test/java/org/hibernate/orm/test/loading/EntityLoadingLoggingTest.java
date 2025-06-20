/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading;

import org.hibernate.sql.results.graph.entity.EntityLoadingLogging;

import org.hibernate.testing.logger.LogInspectionHelper;
import org.hibernate.testing.logger.TriggerOnPrefixLogListener;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel( annotatedClasses = {
		EntityLoadingLoggingTest.Parent.class,
		EntityLoadingLoggingTest.Child.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16234" )
public class EntityLoadingLoggingTest {
	private TriggerOnPrefixLogListener trigger;

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent parent = new Parent( "parent_1" );
			final Child child = new Child( parent );
			session.persist( parent );
			session.persist( child );
		} );
	}

	@BeforeEach
	public void setUp() {
		trigger = new TriggerOnPrefixLogListener( "(EntityResultInitializer) Created new entity instance" );
		LogInspectionHelper.registerListener( trigger, EntityLoadingLogging.ENTITY_LOADING_LOGGER );
	}

	@AfterEach
	public void reset() {
		trigger.reset();
	}

	@AfterAll
	public static void tearDown(SessionFactoryScope scope) {
		LogInspectionHelper.clearAllListeners( EntityLoadingLogging.ENTITY_LOADING_LOGGER );
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Child child = session.find( Child.class, 1L );
			assertThat( child.getParent().getId() ).isEqualTo( "parent_1" );
			// uncomment logger.results-loading-entity.name in log4j2.properties to verify log
			// assertThat( trigger.wasTriggered() ).as( "Log should be triggered" ).isTrue();
		} );
	}

	@Entity( name = "Parent" )
	public static class Parent {
		@Id
		private String id;

		public Parent() {
		}

		public Parent(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}
	}

	@Entity( name = "Child" )
	public static class Child {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne( optional = false )
		private Parent parent;

		public Child() {
		}

		public Child(Parent parent) {
			this.parent = parent;
		}

		public Long getId() {
			return id;
		}

		public Parent getParent() {
			return parent;
		}

		@Override
		public String toString() {
			return id + "-" + parent.getId();
		}
	}
}
