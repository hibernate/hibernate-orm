/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.annotations.formula;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Formula;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-13612")
public class FormulaWithQuotedTableNamesAndQuotedColumnNamesTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { TestEntity.class, TestEntity2.class };
	}

	@Before
	public void setUp() {
		doInHibernate( this::sessionFactory, session -> {
			TestEntity testEntity = new TestEntity();
			testEntity.setId( 1L );
			testEntity.setIntegerValue( 2 );
			session.save( testEntity );

			TestEntity2 testEntity2 = new TestEntity2();
			testEntity2.setAnotherIntegerValue( 1 );
			testEntity2.setSelect( 3 );
			session.save( testEntity2 );
		} );
	}

	@Test
	public void testThatTheTableNamesAreNotQualifiedWithGeneratedAliases() {
		doInHibernate( this::sessionFactory, session -> {
			session.get( TestEntity.class, 1L );
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

		@Formula("( select te.`INTEGER_VALUE` + te2.`select` + te2.`ANOTHER_STRING_VALUE` from `TEST_ENTITY` te, `TEST_ENTITY_2` te2 where te.`ID` = 1L)")
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
