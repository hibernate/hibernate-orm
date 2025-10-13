/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.action.internal.EntityActionVetoException;
import org.hibernate.event.spi.EventType;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Chris Cranford
 */
@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
@JiraKey(value = "HHH-11721")
@DomainModel(
		annotatedClasses = {
				PreInsertEventListenerVetoBidirectionalTest.Child.class,
				PreInsertEventListenerVetoBidirectionalTest.Parent.class
		}
)
@SessionFactory
public class PreInsertEventListenerVetoBidirectionalTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testVeto(SessionFactoryScope scope) {
		scope.getSessionFactory().getEventListenerRegistry()
				.appendListeners(
						EventType.PRE_INSERT,
						event -> event.getEntity() instanceof Parent
				);
		assertThatThrownBy( () -> scope.inTransaction( session -> {
			Parent parent = new Parent();
			parent.setField1( "f1" );
			parent.setfield2( "f2" );

			Child child = new Child();
			parent.setChild( child );

			session.persist( parent );
		} ) ).isInstanceOf( EntityActionVetoException.class );
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Integer id;

		@OneToOne
		private Parent parent;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Integer id;

		private String field1;

		private String field2;

		@OneToOne(cascade = CascadeType.ALL, mappedBy = "parent")
		private Child child;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getField1() {
			return field1;
		}

		public void setField1(String field1) {
			this.field1 = field1;
		}

		public String getField2() {
			return field2;
		}

		public void setfield2(String field2) {
			this.field2 = field2;
		}

		public Child getChild() {
			return child;
		}

		public void setChild(Child child) {
			this.child = child;
			child.setParent( this );
		}
	}
}
