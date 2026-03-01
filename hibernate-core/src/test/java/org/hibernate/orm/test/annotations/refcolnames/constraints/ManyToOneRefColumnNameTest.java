/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.refcolnames.constraints;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.RollbackException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

@Jpa(annotatedClasses = {ManyToOneRefColumnNameTest.This.class, ManyToOneRefColumnNameTest.That.class})
@JiraKey("HHH-19479")
class ManyToOneRefColumnNameTest {

	@Test void test(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		This thisThing = new This();
		That thatThing = new That();
		thatThing.compositeKeyOne = 1;
		thatThing.compositeKeyTwo = 2;
		thatThing.singleKey = "hello";
		thisThing.thatBySingleKey = thatThing;
		thisThing.thatByCompositeKey = thatThing;
		scope.inTransaction( em -> {
			em.persist( thatThing );
			em.persist( thisThing );
		} );
		That thing = new That();
		thing.singleKey = "goodbye";
		thing.compositeKeyOne = 5;
		thing.compositeKeyTwo = 3;
		scope.inTransaction( em -> {
			em.persist( thing );
		} );
	}

	@Test void testUnique(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		This thisThing = new This();
		That thatThing = new That();
		thatThing.compositeKeyOne = 1;
		thatThing.compositeKeyTwo = 2;
		thatThing.singleKey = "hello";
		thisThing.thatBySingleKey = thatThing;
		thisThing.thatByCompositeKey = thatThing;
		scope.inTransaction( em -> {
			em.persist( thatThing );
			em.persist( thisThing );
		} );
		That thing = new That();
		try {
			thing.singleKey = "hello";
			scope.inTransaction( em -> {
				em.persist( thing );
			} );
			fail();
		}
		catch (RollbackException re) {
			assertInstanceOf( ConstraintViolationException.class, re.getCause() );
		}
	}

	@Test void testUniqueKey(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		This thisThing = new This();
		That thatThing = new That();
		thatThing.compositeKeyOne = 1;
		thatThing.compositeKeyTwo = 2;
		thatThing.singleKey = "hello";
		thisThing.thatBySingleKey = thatThing;
		thisThing.thatByCompositeKey = thatThing;
		scope.inTransaction( em -> {
			em.persist( thatThing );
			em.persist( thisThing );
		} );
		That thing = new That();
		thing.singleKey = "goodbye";
		thing.compositeKeyOne = 1;
		thing.compositeKeyTwo = 2;
		try {
			scope.inTransaction( em -> {
				em.persist( thing );
			} );
			fail();
		}
		catch (RollbackException re) {
			assertInstanceOf( ConstraintViolationException.class, re.getCause() );
		}
	}

	@Entity(name = "That")
	static class That {
		@Id @GeneratedValue
		long id;

		@Column(nullable = false)
		String singleKey;

		int compositeKeyOne;
		int compositeKeyTwo;
	}

	@Entity(name = "This")
	static class This {
		@Id @GeneratedValue
		long id;

		@JoinColumn(referencedColumnName = "singleKey")
		@ManyToOne
		That thatBySingleKey;

		@JoinColumn(referencedColumnName = "compositeKeyOne")
		@JoinColumn(referencedColumnName = "compositeKeyTwo")
		@ManyToOne
		That thatByCompositeKey;
	}
}
