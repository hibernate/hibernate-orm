/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.generics;

import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@DomainModel(
		annotatedClasses = {
				TypeVariableTest.SimpleEntity.class,
		},
		extraQueryImportClasses = {
				TypeVariableTest.BasicSetterBasedDto.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-15646")
public class TypeVariableTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SimpleEntity simpleEntity = new SimpleEntity( 1, "Fab", Gender.FEMALE );
					session.persist( simpleEntity );
				}
		);
	}

	@Test
	public void testHqlBasicSetterDynamicInstantiation(SessionFactoryScope scope) {
		scope.getSessionFactory().inTransaction(
				session -> {
					final Query<BasicSetterBasedDto> query = session.createQuery(
							"select new BasicSetterBasedDto( e.gender as gender, e.name as value ) from SimpleEntity e",
							BasicSetterBasedDto.class
					);

					final BasicSetterBasedDto dto = query.getSingleResult();
					assertThat( dto, notNullValue() );

					assertThat( dto.gender, is( Gender.FEMALE ) );
					assertThat( dto.value, is( "Fab" ) );
				}
		);
	}

	public static class BasicSetterBasedDto<E, T> {
		private E gender;
		private T value;

		public BasicSetterBasedDto() {
		}

		public BasicSetterBasedDto(E gender, T value) {
			this.gender = gender;
			this.value = value;
		}

		public E getGender() {
			return gender;
		}

		public void setGender(E gender) {
			this.gender = gender;
		}

		public T getValue() {
			return value;
		}

		public void setValue(T value) {
			this.value = value;
		}
	}

	@Entity(name = "SimpleEntity")
	@Table(name = "mapping_simple_entity")
	public static class SimpleEntity {
		private Integer id;
		private String name;
		private Gender gender;

		public SimpleEntity() {
		}

		public SimpleEntity(Integer id, String name, Gender gender) {
			this.id = id;
			this.name = name;
			this.gender = gender;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}


		@Enumerated(EnumType.STRING)
		public Gender getGender() {
			return gender;
		}

		public void setGender(Gender gender) {
			this.gender = gender;
		}

	}

	public enum Gender {
		MALE,
		FEMALE
	}

}
