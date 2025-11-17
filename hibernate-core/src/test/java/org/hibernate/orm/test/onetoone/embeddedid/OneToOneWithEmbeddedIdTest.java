/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.embeddedid;

import java.io.Serializable;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

/**
 * @author Nathan Xu
 */
@Jpa(
		annotatedClasses = {
				OneToOneWithEmbeddedIdTest.Entity1.class,
				OneToOneWithEmbeddedIdTest.Entity2.class,
				OneToOneWithEmbeddedIdTest.ID1.class,
				OneToOneWithEmbeddedIdTest.ID2.class
		},
		integrationSettings = @Setting(name = AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, value = "true")
)
@JiraKey(value = "HHH-15153")
class OneToOneWithEmbeddedIdTest {

	@Test
	void testNoExceptionThrown(EntityManagerFactoryScope scope) {

		ID1 id1 = new ID1();
		ID2 id2 = new ID2();

		Entity1 entity1 = new Entity1( id1 );
		Entity2 entity2 = new Entity2( id2 );

		entity2.entity1 = entity1;

		scope.inTransaction( entityManager -> {
			entityManager.persist( entity1 );
			entityManager.persist( entity2 );
		} );

		entity1.value = 1;
		entity2.value = 2;

		// without fixing, the following will thrown exception
		scope.inTransaction( entityManager -> {
			entityManager.merge( entity1 );
			entityManager.merge( entity2 );
		} );
	}


	@Entity(name = "Entity1")
	static class Entity1 {
		@EmbeddedId
		ID1 id;

		@OneToOne(mappedBy = "entity1", cascade = CascadeType.ALL)
		Entity2 entity2;

		Integer value;

		Entity1() {}
		Entity1(ID1 id) {
			this.id = id;
		}

	}

	@Entity(name = "Entity2")
	static class Entity2 {

		@EmbeddedId
		ID2 id;

		@OneToOne(optional = false)
		Entity1 entity1;

		Integer value;

		Entity2() {}
		Entity2(ID2 id) {
			this.id = id;
		}

	}

	@Embeddable
	static class ID1 implements Serializable {
		String id = SafeRandomUUIDGenerator.safeRandomUUIDAsString();
	}

	@Embeddable
	static class ID2 implements Serializable {
		String id = SafeRandomUUIDGenerator.safeRandomUUIDAsString();
	}

}
