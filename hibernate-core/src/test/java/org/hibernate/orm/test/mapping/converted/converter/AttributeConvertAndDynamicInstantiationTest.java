/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.net.URI;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.TypedQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		annotatedClasses = { AttributeConvertAndDynamicInstantiationTest.TestEntity.class }
)
@SessionFactory
@JiraKey(value = "HHH-15331")
public class AttributeConvertAndDynamicInstantiationTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity entity = new TestEntity();
					entity.setLabel( "hibernate" );
					entity.setUri( URI.create( "https://hibernate.org/orm/" ) );
					session.persist( entity );
				}
		);
	}

	@Test
	public void testDynamicInstantiation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final TypedQuery<TestEntity> eQuery = session.createQuery(
							"FROM TestEntity AS e",
							TestEntity.class
					);
					assertEquals( 1, eQuery.getResultList().size() );

					final TypedQuery<TestEntityDTO> dtoQuery = session.createQuery(
							"SELECT new org.hibernate.orm.test.mapping.converted.converter.AttributeConvertAndDynamicInstantiationTest$TestEntityDTO(e.label , e.uri ) FROM TestEntity AS e",
							TestEntityDTO.class
					);
					assertEquals( 1, dtoQuery.getResultList().size() );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private long id;

		@Convert(converter = URIConverter.class)
		private URI uri;

		private String label;

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public URI getUri() {
			return uri;
		}

		public void setUri(URI uri) {
			this.uri = uri;
		}

		public long getId() {
			return id;
		}
	}

	@Converter
	public static class URIConverter implements AttributeConverter<URI, String> {

		@Override
		public String convertToDatabaseColumn(URI attribute) {
			if ( attribute != null ) {
				return attribute.toString();
			}
			return null;
		}

		@Override
		public URI convertToEntityAttribute(String dbData) {
			if ( dbData != null ) {
				return URI.create( dbData );
			}
			return null;
		}

	}

	public static class TestEntityDTO {
		private String label;

		private URI uri;

		public TestEntityDTO(String label, URI uri) {
			this.label = label;
			this.uri = uri;
		}
	}

}
