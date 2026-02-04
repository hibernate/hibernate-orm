/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.enumerated;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.Arrays;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				EnumeratedAndConvertorTest.TestEntity.class,
		}
)
@SessionFactory
@JiraKey(value = "HHH-20119")
public class EnumeratedAndConvertorTest {

	@Test
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity entity = new TestEntity( 1l );
					entity.setState( TestEnum.BAR );
					session.persist( entity );
				}
		);

		scope.inTransaction(
				session -> {
					TestEntity entity = session.get( TestEntity.class, 1l );
					assertThat( entity ).isNotNull();
					assertThat( entity.getState() ).isEqualTo( TestEnum.BAR );
				}
		);
	}

	public enum TestEnum {
		FOO("Label for 'foo'"),
		BAR("Label for 'bar'");

		private final String value;

		TestEnum(String value) {
			this.value = value;
		}
	}

	static class TestConvertor implements AttributeConverter<TestEnum, String> {
		@Override
		public String convertToDatabaseColumn(TestEnum attribute) {
			return attribute.value;
		}

		@Override
		public TestEnum convertToEntityAttribute(String s) {
			return Arrays.stream( TestEnum.values() ).filter( v -> v.value.equals( s ) ).findFirst().get();
		}
	}

	@Entity(name="TestEntity")
	public static class TestEntity {

		@Id
		private long id;

		@Convert(converter = TestConvertor.class)
		private TestEnum state;

		protected TestEntity() {}

		public TestEntity(long id) {
			this.id = id;
		}

		public TestEnum getState() {
			return state;
		}

		public void setState(TestEnum state) {
			this.state = state;
		}
	}
}
