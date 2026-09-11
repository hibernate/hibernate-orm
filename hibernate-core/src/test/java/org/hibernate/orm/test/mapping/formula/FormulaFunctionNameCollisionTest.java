/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.formula;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Formula;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel( annotatedClasses = {
		FormulaFunctionNameCollisionTest.Parent.class,
		FormulaFunctionNameCollisionTest.Detail.class
} )
@SessionFactory
@RequiresDialect(H2Dialect.class)
@JiraKey("HHH-20840")
class FormulaFunctionNameCollisionTest {

	@Test
	void testColumnNamedSysdateInCorrelatedSubquery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Parent( 1, 1 ) );
			session.persist( new Parent( 2, 2 ) );
			session.persist( new Detail( 1, 1, 10 ) );
			session.persist( new Detail( 2, 2, 99 ) );
		} );
		scope.inTransaction( session -> {
			assertEquals( 10, session.find( Parent.class, 1 ).maximumAmount );
			assertEquals( 99, session.find( Parent.class, 2 ).maximumAmount );
		} );
	}

	@Entity(name = "FormulaParent")
	@Table(name = "formula_parent")
	static class Parent {
		@Id
		private Integer id;

		private int sysdate;

		@Formula("(select max(d.amount) from formula_detail d where d.sysdate = sysdate)")
		private Integer maximumAmount;

		Parent() {
		}

		Parent(Integer id, int sysdate) {
			this.id = id;
			this.sysdate = sysdate;
		}
	}

	@Entity(name = "FormulaDetail")
	@Table(name = "formula_detail")
	static class Detail {
		@Id
		private Integer id;

		private int sysdate;
		private int amount;

		Detail() {
		}

		Detail(Integer id, int sysdate, int amount) {
			this.id = id;
			this.sysdate = sysdate;
			this.amount = amount;
		}
	}
}
