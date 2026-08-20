/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.util.AbstractMap;
import java.util.Set;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a Map subtype whose type parameters are resolved through
 * a generic intermediate class (with fewer type params than Map) can be
 * properly bootstrapped and persisted as JSON.
 */
@Jira("https://hibernate.atlassian.net/browse/HHH-20597")
@DomainModel(annotatedClasses = JsonGenericMapSubtypeTest.TestEntity.class)
@SessionFactory
public class JsonGenericMapSubtypeTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var entity = new TestEntity();
			entity.id = 1L;
			entity.payload = new JsonPayload();
			entity.payload.put( "key1", "value1" );
			entity.payload.put( "key2", "value2" );
			session.persist( entity );
		} );
	}

	@Test
	public void testReadWorks(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var entity = session.find( TestEntity.class, 1L );
			assertThat( entity ).isNotNull();
			assertThat( entity.payload ).containsEntry( "key1", "value1" );
			assertThat( entity.payload ).containsEntry( "key2", "value2" );
			assertThat( entity.payload ).hasSize( 2 );
		} );
	}

	public static abstract class GenericStringMap<V> extends AbstractMap<String, V> {

		private final java.util.HashMap<String, V> delegate = new java.util.HashMap<>();

		@Override
		public Set<Entry<String, V>> entrySet() {
			return delegate.entrySet();
		}

		@Override
		public V put(String key, V value) {
			return delegate.put( key, value );
		}
	}

	public static class JsonPayload extends GenericStringMap<Object> {
	}

	@Entity(name = "JsonGenericMapSubtypeEntity")
	public static class TestEntity {
		@Id
		Long id;

		@JdbcTypeCode(SqlTypes.JSON)
		JsonPayload payload;
	}
}
