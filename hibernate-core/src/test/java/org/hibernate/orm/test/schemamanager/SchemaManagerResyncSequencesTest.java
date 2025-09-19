/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemamanager;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = SchemaManagerResyncSequencesTest.EntityWithSequence.class)
class SchemaManagerResyncSequencesTest {
	@Test void test(SessionFactoryScope scope) {
		var schemaManager = scope.getSessionFactory().getSchemaManager();
		scope.inStatelessTransaction( ss -> {
			ss.upsert( new EntityWithSequence(50L, "x") );
			ss.upsert( new EntityWithSequence(100L, "y") );
			ss.upsert( new EntityWithSequence(200L, "z") );
		} );
		schemaManager.resynchronizeSequences();
		scope.inStatelessTransaction( ss -> {
			var entity = new EntityWithSequence();
			ss.insert( entity );
			assertEquals(201L, entity.id);
		});
	}
	@Entity(name = "EntityWithSequence")
	static class EntityWithSequence {
		@Id
		@GeneratedValue
		@SequenceGenerator(name = "TheSequence", allocationSize = 20)
		Long id;
		String name;

		EntityWithSequence(Long id, String name) {
			this.id = id;
			this.name = name;
		}
		EntityWithSequence() {
		}
	}
}
