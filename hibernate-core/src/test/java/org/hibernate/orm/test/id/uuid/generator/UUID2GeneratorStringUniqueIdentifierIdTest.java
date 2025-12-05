/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.uuid.generator;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect(SQLServerDialect.class)
@JiraKey("HHH-12943")
@DomainModel(annotatedClasses = UUID2GeneratorStringUniqueIdentifierIdTest.FooEntity.class)
@SessionFactory
public class UUID2GeneratorStringUniqueIdentifierIdTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testPaginationQuery(SessionFactoryScope factoryScope) {
		var fooId = factoryScope.fromTransaction( (session) -> {
			FooEntity entity = new FooEntity();
			entity.getFooValues().add("one");
			entity.getFooValues().add("two");
			entity.getFooValues().add("three");

			session.persist(entity);

			return entity.getId();
		} );

		assertNotNull( fooId );

		factoryScope.inTransaction( session -> {
			FooEntity entity = session.find(FooEntity.class, fooId.toUpperCase());
			assertNotNull(entity);

			assertEquals( 3, entity.getFooValues().size() );
		} );

		factoryScope.inTransaction( entityManager -> {
			FooEntity entity = entityManager.find(FooEntity.class, fooId);
			assertNotNull(entity);

			assertEquals( 3, entity.getFooValues().size() );
		} );
	}

	@Entity
	@Table(name = "foo")
	public static class FooEntity {

		@Id
		@GenericGenerator(name = "uuid", strategy = "uuid2")
		@GeneratedValue(generator = "uuid")
		@Column(columnDefinition = "UNIQUEIDENTIFIER")
		private String id;

		@ElementCollection
		@JoinTable(name = "foo_values")
		@Column(name = "foo_value")
		private final Set<String> fooValues = new HashSet<>();

		public String getId() {
			return id.toUpperCase();
		}

		public void setId(String id) {
			this.id = id;
		}

		public Set<String> getFooValues() {
			return fooValues;
		}

	}
}
