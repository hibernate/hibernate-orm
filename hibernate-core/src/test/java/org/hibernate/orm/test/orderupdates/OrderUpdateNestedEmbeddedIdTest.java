/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orderupdates;

import java.io.Serializable;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		OrderUpdateNestedEmbeddedIdTest.ParentId.class,
		OrderUpdateNestedEmbeddedIdTest.Parent.class,
		OrderUpdateNestedEmbeddedIdTest.Child1Id.class,
		OrderUpdateNestedEmbeddedIdTest.Child1.class,
		OrderUpdateNestedEmbeddedIdTest.Child2.class,
} )
@ServiceRegistry( settings = @Setting( name = AvailableSettings.ORDER_UPDATES, value = "true" ) )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16709" )
public class OrderUpdateNestedEmbeddedIdTest {
	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Child2" ).executeUpdate();
			session.createMutationQuery( "delete from Child1" ).executeUpdate();
			session.createMutationQuery( "delete from Parent" ).executeUpdate();
		} );
	}

	@Test
	public void testParentPersist(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ParentId parentId = new ParentId( "parent_1" );
			final Child1Id child1Id1 = new Child1Id( parentId, 1 );
			final Child1Id child1Id2 = new Child1Id( parentId, 2 );
			final Parent parent = new Parent(
					parentId,
					List.of( new Child1( child1Id1, List.of() ), new Child1( child1Id2, List.of( new Child2() ) ) )
			);
			session.persist( parent );
		} );
	}

	@Embeddable
	public static class ParentId implements Serializable {
		private String id;

		public ParentId() {
		}

		public ParentId(String id) {
			this.id = id;
		}
	}

	@Entity( name = "Parent" )
	public static class Parent {
		@EmbeddedId
		private ParentId parentId;

		@OneToMany( cascade = CascadeType.ALL )
		@JoinColumn( name = "parent_id", referencedColumnName = "id" )
		private List<Child1> child1s;

		public Parent() {
		}

		public Parent(ParentId parentId, List<Child1> child1s) {
			this.parentId = parentId;
			this.child1s = child1s;
		}
	}

	@Embeddable
	public static class Child1Id implements Serializable {
		@Embedded
		private ParentId parentId;
		private Integer version;

		public Child1Id() {
		}

		public Child1Id(ParentId parentId, Integer version) {
			this.parentId = parentId;
			this.version = version;
		}
	}

	@Entity( name = "Child1" )
	public static class Child1 {
		@EmbeddedId
		private Child1Id child1Id;

		@OneToMany( cascade = CascadeType.ALL )
		@JoinColumn( name = "child1_id", referencedColumnName = "id" )
		@JoinColumn( name = "child1_version", referencedColumnName = "version" )
		private List<Child2> child2s;

		public Child1() {
		}

		public Child1(Child1Id child1Id, List<Child2> child2s) {
			this.child1Id = child1Id;
			this.child2s = child2s;
		}
	}

	@Entity( name = "Child2" )
	public static class Child2 {
		@Id
		@GeneratedValue
		private Long id;
	}
}
