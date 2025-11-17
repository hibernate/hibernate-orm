/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.various;

import org.hibernate.annotations.Subselect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				OneOneGeneratedValueTest.EntityB.class,
				OneOneGeneratedValueTest.EntityA.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-15520")
public class OneOneGeneratedValueTest {

	@Test
	public void testIt(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA entityA = new EntityA( 1L );
					session.persist( entityA );
				}
		);
		scope.inTransaction(
				session -> {
					EntityA entityA = session.get( EntityA.class, 1L );
					assertThat( entityA ).isNotNull();
					EntityB entityB = entityA.getB();
					assertThat( entityB ).isNotNull();
					assertThat( entityB.getB() ).isEqualTo( 5L );
				}
		);
	}

	@Entity(name = "EntityA")
	@Table(name = "TABLE_A")
	public static class EntityA {

		@Id
		private Long id;

		private String name;

		@OneToOne(mappedBy = "a")
		private EntityB b;

		public EntityA() {
		}

		public EntityA(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public EntityB getB() {
			return b;
		}
	}

	@Entity(name = "EntityB")
	@Subselect("SELECT 5 as b, a.id AS AId FROM TABLE_A a")
	public static class EntityB {

		private Long aId;

		private EntityA a;

		private Long b;

		@Id
		public Long getAId() {
			return aId;
		}

		public void setAId(Long aId) {
			this.aId = aId;
		}

		@OneToOne
		@PrimaryKeyJoinColumn
		public EntityA getA() {
			return a;
		}

		public void setA(EntityA a) {
			this.a = a;
		}

		public Long getB() {
			return b;
		}

		public void setB(Long b) {
			this.b = b;
		}


	}
}
