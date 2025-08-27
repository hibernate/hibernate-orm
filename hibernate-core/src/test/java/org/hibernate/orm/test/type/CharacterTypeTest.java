/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DomainModel(
		annotatedClasses = CharacterTypeTest.TestEntity.class
)
@SessionFactory
public class CharacterTypeTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity dataTypes = new TestEntity( 1, ' ' );
					session.persist( dataTypes );
				}
		);
	}

	@Test
	public void transientTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity d1 = session.find( TestEntity.class, 1 );
					assertNotNull( d1 );
					assertEquals( ' ', d1.getCharacterData() );
					d1.setCharacterData( null );
				}
		);

		scope.inTransaction(
				session -> {
					TestEntity d1 = session.find( TestEntity.class, 1 );
					assertNotNull( d1 );
					assertNull( d1.getCharacterData() );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {

		@Id
		private Integer id;

		private Character characterData;

		public TestEntity() {
		}

		public TestEntity(Integer id, Character characterData) {
			this.id = id;
			this.characterData = characterData;
		}

		public Integer getId() {
			return id;
		}

		public void setCharacterData(Character characterData) {
			this.characterData = characterData;
		}

		public Character getCharacterData() {
			return characterData;
		}
	}

}
