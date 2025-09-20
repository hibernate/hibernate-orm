/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemamanager;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.TableGenerator;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = SchemaManagerResyncTablesTest.EntityWithTable.class)
class SchemaManagerResyncTablesTest {
	@Test void test(SessionFactoryScope scope) {
		var schemaManager = scope.getSessionFactory().getSchemaManager();
		scope.inStatelessTransaction( ss -> {
			ss.upsert( new EntityWithTable(50L, "x") );
			ss.upsert( new EntityWithTable(100L, "y") );
			ss.upsert( new EntityWithTable(200L, "z") );
		} );
		schemaManager.resynchronizeGenerators();
		scope.inStatelessTransaction( ss -> {
			var entity = new EntityWithTable();
			ss.insert( entity );
			assertEquals(201L, entity.id);
		});
	}
	@Entity(name = "EntityWithTable")
	static class EntityWithTable {
		@Id
		@GeneratedValue
		@TableGenerator(name = "TheTable", allocationSize = 20)
		Long id;
		String name;

		EntityWithTable(Long id, String name) {
			this.id = id;
			this.name = name;
		}
		EntityWithTable() {
		}
	}
}
