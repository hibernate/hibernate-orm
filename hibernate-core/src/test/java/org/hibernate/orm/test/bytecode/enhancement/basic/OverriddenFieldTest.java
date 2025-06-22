/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;

/**
 * Tests persisting and then loading a property with bytecode enhancement enabled
 * when the entity has the same field defined twice: once in a mappedsuperclass (should be ignored)
 * and once in the concrete entity class.
 */
@JiraKey("HHH-15505")
@DomainModel(
		annotatedClasses = {
				OverriddenFieldTest.AbstractEntity.class, OverriddenFieldTest.Fruit.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class OverriddenFieldTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Fruit testEntity = new Fruit();
			testEntity.setId( 1 );
			testEntity.setName( "John" );
			s.persist( testEntity );
		} );

		scope.inTransaction( s -> {
			Fruit testEntity = s.get( Fruit.class, 1 );
			assertEquals( "John", testEntity.getName() );
		} );
	}

	@MappedSuperclass
	public static class AbstractEntity {

		@Column(length = 40, unique = true)
		private String name;

	}

	@Entity
	@Table(name = "known_fruits")
	public static class Fruit extends AbstractEntity {

		@Id
		private Integer id;

		@Column(length = 40, unique = true)
		private String name;

		public Fruit() {
		}

		public Fruit(String name) {
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
