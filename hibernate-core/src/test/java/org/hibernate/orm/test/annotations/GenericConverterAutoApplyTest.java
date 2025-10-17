/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.type.BasicType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Yanming Zhou
 */
@JiraKey("HHH-18012")
@Jpa(annotatedClasses = {
		GenericConverterAutoApplyTest.IntegerArrayConverter.class,
		GenericConverterAutoApplyTest.IntegerListConverter.class,
		GenericConverterAutoApplyTest.TestEntity.class}
)
public class GenericConverterAutoApplyTest {

	@Test
	public void genericArrayIsAutoApplied(EntityManagerFactoryScope scope) {
		assertAttributeIsMappingToString( scope.getEntityManagerFactory().createEntityManager(), "integerArray" );
	}

	@Test
	public void genericListIsAutoApplied(EntityManagerFactoryScope scope) {
		assertAttributeIsMappingToString( scope.getEntityManagerFactory().createEntityManager(), "integerList" );
	}

	private void assertAttributeIsMappingToString(EntityManager em, String name) {
		EntityType<?> entityType = em.getMetamodel().entity(TestEntity.class);
		assertThat(entityType.getAttribute(name)).isInstanceOfSatisfying(SingularAttribute.class,
				sa -> assertThat(sa.getType()).isInstanceOfSatisfying(BasicType.class,
						bt -> assertThat(bt.getJdbcJavaType().getJavaType()).isEqualTo(String.class)
				));
	}

	static abstract class AbstractArrayConverter<T> implements AttributeConverter<T[], String> {

		@Override
		public String convertToDatabaseColumn(T[] array) {
			return null;
		}

		@Override
		public T[] convertToEntityAttribute(String string) {
			return null;
		}
	}

	@Converter(autoApply = true)
	static class IntegerArrayConverter extends AbstractArrayConverter<Integer> {

	}

	static abstract class AbstractListConverter<T> implements AttributeConverter<List<T>, String> {

		@Override
		public String convertToDatabaseColumn(List<T> array) {
			return null;
		}

		@Override
		public List<T> convertToEntityAttribute(String string) {
			return null;
		}

	}

	@Converter(autoApply = true)
	static class IntegerListConverter extends AbstractListConverter<Integer> {

	}

	@Entity
	static class TestEntity {

		@Id
		@GeneratedValue
		Long id;

		Integer[] integerArray;

		List<Integer> integerList;
	}

}
