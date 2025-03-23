/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.generics;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@DomainModel(
		annotatedClasses = {
				MappedSuperclassAndGenericsTest.RawEntity.class,
				MappedSuperclassAndGenericsTest.LongEntity.class
		}
)
@SessionFactory
@JiraKey( value = "HHH-15970")
public class MappedSuperclassAndGenericsTest {

	public enum MyEnum {
		A, B
	}

	@Test
	public void testIt(SessionFactoryScope scope){
		scope.inTransaction(
			session -> {

			}
		);
	}

	@MappedSuperclass
	public static abstract class ParameterizedParent<L> {

		@Enumerated(EnumType.STRING)
		private MyEnum myEnum;

		public MyEnum getMyEnum() {
			return myEnum;
		}

		public void setMyEnum(MyEnum myEnum) {
			this.myEnum = myEnum;
		}

	}

	@Entity(name = "RawEntity")
	public static class RawEntity extends ParameterizedParent {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		@Column(name = "id", nullable = false)
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "LongEntity")
	public static class LongEntity extends ParameterizedParent<Long> {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		@Column(name = "id", nullable = false)
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

}
