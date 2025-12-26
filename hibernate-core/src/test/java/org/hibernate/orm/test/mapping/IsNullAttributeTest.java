/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping;



import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.processing.Exclude;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;


/**
 * @author Guillaume Toison
 */
@JiraKey( "HHH-18939" )
@DomainModel(
		annotatedClasses = { IsNullAttributeTest.SimpleEntity.class }
)
@ServiceRegistry
@SessionFactory
public class IsNullAttributeTest {

	@Test
	public void testSimpleEntity(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.persist( new SimpleEntity() );
		});
	}

	@Entity(name = "SimpleEntity")
	@Table(name = "mapping_simple_entity")
	public static class SimpleEntity {
		private Integer id;
		private Component component;
		private SimpleComponent simpleComponent;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Embedded
		public Component getComponent() {
			return component;
		}

		public void setComponent(Component component) {
			this.component = component;
		}

		@Embedded
		public SimpleComponent getSimpleComponent() {
			return simpleComponent;
		}

		public void setSimpleComponent(SimpleComponent simpleComponent) {
			this.simpleComponent = simpleComponent;
		}
	}

	@Embeddable
	public static class Component {
		private SimpleEntity a;
		private SubComponent attribute1;
		private SimpleEntity entity;

		public Component() {
		}

		public Component(SubComponent attribute1) {
			this.attribute1 = attribute1;
		}

		@ManyToOne
		@JoinColumn(name = "A_id")
		public SimpleEntity getA() {
			return a;
		}

		public void setA(SimpleEntity a) {
			this.a = a;
		}

		@Embedded
		public SubComponent getAttribute1() {
			return attribute1;
		}

		public void setAttribute1(SubComponent attribute1) {
			this.attribute1 = attribute1;
		}

		@ManyToOne
		@JoinColumn(name = "entity_id")
		public SimpleEntity getEntity() {
			return entity;
		}

		public void setEntity(SimpleEntity entity) {
			this.entity = entity;
		}
	}


	@Embeddable
	public static class SimpleComponent {
		private String value;

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	@Embeddable
	@Exclude // remove this to reproduce the metamodel generation issue
	public static class SubComponent {
		private String attribute1;

		public SubComponent() {
		}

		public SubComponent(String attribute1) {
			this.attribute1 = attribute1;
		}

		public String getAttribute1() {
			return attribute1;
		}

		public void setAttribute1(String attribute1) {
			this.attribute1 = attribute1;
		}

		public boolean isNullx() {
			return attribute1 == null;
		}
	}
}
