/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class EnumerationConverterTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class
		};
	}

	@Test
	public void test() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Person person = new Person();
			person.setId(1L);
			person.setName("John Doe");
			person.setGender(Gender.MALE);
			entityManager.persist(person);
		});
	}

	//tag::basic-enums-attribute-converter-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String name;

		@Convert(converter = GenderConverter.class)
		public Gender gender;

		//Getters and setters are omitted for brevity

	//end::basic-enums-attribute-converter-example[]
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Gender getGender() {
			return gender;
		}

		public void setGender(Gender gender) {
			this.gender = gender;
		}
	//tag::basic-enums-attribute-converter-example[]
	}

	@Converter
	public static class GenderConverter
			implements AttributeConverter<Gender, Character> {

		public Character convertToDatabaseColumn(Gender value) {
			if (value == null) {
				return null;
			}

			return value.getCode();
		}

		public Gender convertToEntityAttribute(Character value) {
			if (value == null) {
				return null;
			}

			return Gender.fromCode(value);
		}
	}
	//end::basic-enums-attribute-converter-example[]
}
