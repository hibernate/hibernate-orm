/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		ConvertedEmbeddableCollectionTest.TestEmbeddable.class,
		ConvertedEmbeddableCollectionTest.TestEntity.class,
} )
@SessionFactory
public class ConvertedEmbeddableCollectionTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity entity = new TestEntity( 1L );
			entity.getEmbeddables().add( new TestEmbeddable( "test" ) );
			session.persist( entity );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from TestEntity" ).executeUpdate() );
	}

	@Test
	public void testMapping(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat(
				session.find( TestEntity.class, 1L )
						.getEmbeddables()
						.stream()
						.map( TestEmbeddable::getData )
		).containsExactly( "test" ) );
	}

	@Embeddable
	public static class TestEmbeddable {
		private String data;

		public TestEmbeddable() {
		}

		public TestEmbeddable(String data) {
			this.data = data;
		}

		public String getData() {
			return data;
		}
	}

	@Entity( name = "TestEntity" )
	public static class TestEntity {
		@Id
		private Long id;

		@Convert( converter = EmbeddableConverter.class )
		private Set<TestEmbeddable> embeddables = new HashSet<>();

		public TestEntity() {
		}

		public TestEntity(Long id) {
			this.id = id;
		}

		public Set<TestEmbeddable> getEmbeddables() {
			return embeddables;
		}
	}

	@Converter
	public static class EmbeddableConverter implements AttributeConverter<TestEmbeddable, String> {
		@Override
		public String convertToDatabaseColumn(TestEmbeddable attribute) {
			return attribute.getData();
		}

		@Override
		public TestEmbeddable convertToEntityAttribute(String dbData) {
			return new TestEmbeddable( dbData );
		}
	}
}
