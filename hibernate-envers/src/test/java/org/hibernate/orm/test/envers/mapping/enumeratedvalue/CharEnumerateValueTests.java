/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.mapping.enumeratedvalue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumeratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.type.SqlTypes;
import org.junit.Test;


@JiraKey( "HHH-19747" )
public class CharEnumerateValueTests extends EntityManagerFactoryBasedFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {Person.class};
	}

	@Test
	public void testBasicUsage() {
		final EntityManagerFactory testEmf = produceEntityManagerFactory();
		testEmf.close();
	}

	public enum Gender {
		MALE( 'M' ),
		FEMALE( 'F' ),
		OTHER( 'U' );

		@EnumeratedValue
		private final char code;

		Gender(char code) {
			this.code = code;
		}

		public char getCode() {
			return code;
		}
	}

	@Audited
	@Entity(name = "Person")
	@Table(name = "persons")
	public static class Person {
		@Id
		private Integer id;
		private String name;
		@Enumerated(EnumType.STRING)
		@JdbcTypeCode(SqlTypes.CHAR)
		@Column(length = 1)
		private Gender gender;

		public Person() {
		}

		public Person(Integer id, String name, Gender gender) {
			this.id = id;
			this.name = name;
			this.gender = gender;
		}
	}
}
