/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.generics;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that converter hierarchies with the same generic parameter name at multiple
 * levels (e.g. {@code T extends Enum<T>}) do not cause StackOverflowError.
 */
@JiraKey("HHH-20624")
@DomainModel(
		annotatedClasses = {
				GenericConverterHierarchyTest.TestEntity.class,
				GenericConverterHierarchyTest.StatusConverter.class
		}
)
@SessionFactory
public class GenericConverterHierarchyTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testConverterHierarchyWithSameGenericParameterName(SessionFactoryScope scope) {
		final EntityPersister ep = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( TestEntity.class.getName() );
		final Type statusPropertyType = ep.getPropertyType( "status" );
		final ConvertedBasicTypeImpl convertedType = assertTyping(
				ConvertedBasicTypeImpl.class,
				statusPropertyType
		);
		final JpaAttributeConverter converter = (JpaAttributeConverter) convertedType.getValueConverter();
		assertTrue( StatusConverter.class.isAssignableFrom( converter.getConverterJavaType().getJavaTypeClass() ) );

		scope.inTransaction( session -> session.persist( new TestEntity( 1L, Status.INACTIVE ) ) );

		scope.inSession( session -> {
			final String storedValue = session.createNativeQuery( "select status from TestEntity where id = 1", String.class ).getSingleResult();
			assertEquals( "INACTIVE", storedValue );
		} );

		scope.inTransaction( session -> {
			final TestEntity entity = session.find( TestEntity.class, 1L );

			assertNotNull( entity );
			assertEquals( Status.INACTIVE, entity.getStatus() );
		} );

	}

	public enum Status {
		ACTIVE,
		INACTIVE
	}

	@Converter
	public static abstract class AbstractJpaEnumConverter<T extends Enum<T>>
			implements AttributeConverter<T, String> {

		@Override
		public String convertToDatabaseColumn(T attribute) {
			return attribute == null ? null : attribute.name();
		}

		@Override
		public T convertToEntityAttribute(String dbData) {
			return dbData == null ? null : convertToEnum( dbData );
		}

		protected abstract T convertToEnum(String dbData);
	}

	@Converter
	public static abstract class SpecialJpaEnumConverter<T extends Enum<T>>
			extends AbstractJpaEnumConverter<T> {
	}

	@Converter(autoApply = true)
	public static class StatusConverter
			extends SpecialJpaEnumConverter<Status> {

		@Override
		protected Status convertToEnum(String dbData) {
			return Status.valueOf( dbData );
		}
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {

		@Id
		private Long id;

		private Status status;

		public TestEntity() {
		}

		public TestEntity(Long id, Status status) {
			this.id = id;
			this.status = status;
		}

		public Status getStatus() {
			return status;
		}
	}
}
