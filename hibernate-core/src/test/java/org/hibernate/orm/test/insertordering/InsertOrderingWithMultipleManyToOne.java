/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-11996")
public class InsertOrderingWithMultipleManyToOne extends BaseInsertOrderingTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Parent.class,
				ChildA.class,
				ChildB.class,
		};
	}

	@Test
	public void testBatching() {
		sessionFactoryScope().inTransaction( session -> {
			Parent parent = new Parent();
			session.persist( parent );

			ChildA childA = new ChildA();
			childA.setParent( parent );
			session.persist( childA );

			ChildB childB = new ChildB();
			childB.setParent( parent );
			session.persist( childB );

			clearBatches();
		} );

		verifyPreparedStatementCount( 3 );
		/*
		Map<String, Integer> expectedBatching = new HashMap<>();
		expectedBatching.put( "insert into Address (ID) values (?)", 2 );
		expectedBatching.put( "insert into Person (ID) values (?)", 4 );
		verifyBatching( expectedBatching );
		*/
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		@GeneratedValue
		private Integer id;

		private String name;

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
	}

	@Entity(name = "ChildA")
	public static class ChildA {
		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		@ManyToOne
		private Parent parent;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "ChildB")
	public static class ChildB {
		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		@ManyToOne
		private Parent parent;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
