/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.formula;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Formula;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@JiraKey("HHH-13612")
@DomainModel(
		annotatedClasses = {
				FormulaWithQuotedTableNamesAndQuotedColumnNamesTest.TestEntity.class,
				FormulaWithQuotedTableNamesAndQuotedColumnNamesTest.TestEntity2.class
		}
)
@SessionFactory
public class FormulaWithQuotedTableNamesAndQuotedColumnNamesTest {

	private final static Long TEST_ENTITY_ID = 1L;

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			TestEntity testEntity = new TestEntity();
			testEntity.setId( TEST_ENTITY_ID );
			testEntity.setIntegerValue( 2 );
			session.persist( testEntity );

			TestEntity2 testEntity2 = new TestEntity2();
			testEntity2.setAnotherIntegerValue( 1 );
			testEntity2.setSelect( 3 );
			session.persist( testEntity2 );
		} );
	}

	@Test
	public void testThatTheTableNamesAreNotQualifiedWithGeneratedAliases(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			TestEntity testEntity = session.get( TestEntity.class, TEST_ENTITY_ID );
			assertThat( testEntity ).isNotNull();
		} );
	}


	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {
		@Id
		@Column(name = "ID")
		private Long id;

		@Column(name = "INTEGER_VALUE")
		private Integer integerValue;

		@Formula(
				"( select te.INTEGER_VALUE + te2.`select` + te2.ANOTHER_STRING_VALUE from TEST_ENTITY te, TEST_ENTITY_2 te2 where te.ID = 1)"
		)
		private Integer computedIntegerValue;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Integer getComputedIntegerValue() {
			return computedIntegerValue;
		}

		public void setComputedIntegerValue(Integer computedIntegerValue) {
			this.computedIntegerValue = computedIntegerValue;
		}

		public Integer getIntegerValue() {
			return integerValue;
		}

		public void setIntegerValue(Integer integerValue) {
			this.integerValue = integerValue;
		}
	}

	@Entity(name = "TestEntity2")
	@Table(name = "TEST_ENTITY_2")
	public static class TestEntity2 {
		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "ANOTHER_STRING_VALUE")
		private Integer anotherIntegerValue;

		@Column(name = "`select`")
		private Integer select;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Integer getAnotherIntegerValue() {
			return anotherIntegerValue;
		}

		public void setAnotherIntegerValue(Integer anotherIntegerValue) {
			this.anotherIntegerValue = anotherIntegerValue;
		}

		public Integer getSelect() {
			return select;
		}

		public void setSelect(Integer select) {
			this.select = select;
		}
	}
}
