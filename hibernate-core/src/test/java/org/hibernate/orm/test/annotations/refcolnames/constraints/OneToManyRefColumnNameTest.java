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
import jakarta.persistence.OneToMany;
import jakarta.persistence.RollbackException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@Jpa(annotatedClasses = {OneToManyRefColumnNameTest.This.class, OneToManyRefColumnNameTest.That.class})
@JiraKey("HHH-19480")
class OneToManyRefColumnNameTest {

	@Test void test(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		This thisThing = new This();
		That thatThing = new That();
		thatThing.compositeKeyOne = 1;
		thatThing.compositeKeyTwo = 2;
		thatThing.singleKey = "hello";
		thatThing.theseOnSingleKey.add( thisThing );
		thatThing.theseOnCompositeKey.add( thisThing );
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

	@Test void testForeignKey(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		This thisThing = new This();
		That thatThing = new That();
		thatThing.compositeKeyOne = 1;
		thatThing.compositeKeyTwo = 2;
		thatThing.singleKey = "hello";
		thatThing.theseOnSingleKey.add( thisThing );
		thatThing.theseOnCompositeKey.add( thisThing );
		scope.inTransaction( em -> {
			em.persist( thatThing );
			em.persist( thisThing );
		} );

		assertThrows( ConstraintViolationException.class, () ->
				scope.inTransaction( em -> {
					em.createQuery( "update That set singleKey = 'goodbye'" ).executeUpdate();
				} )
		);

		assertThrows( ConstraintViolationException.class, () ->
				scope.inTransaction( em -> {
					em.createQuery( "update That set compositeKeyOne = 69" ).executeUpdate();
				} )
		);
	}

	@Test void testUnique(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		This thisThing = new This();
		That thatThing = new That();
		thatThing.compositeKeyOne = 1;
		thatThing.compositeKeyTwo = 2;
		thatThing.singleKey = "hello";
		thatThing.theseOnSingleKey.add( thisThing );
		thatThing.theseOnCompositeKey.add( thisThing );
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
		thatThing.theseOnSingleKey.add( thisThing );
		thatThing.theseOnCompositeKey.add( thisThing );
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

		@OneToMany
		@JoinColumn(referencedColumnName = "singleKey")
		Set<This> theseOnSingleKey = new HashSet<>();

		@OneToMany
		@JoinColumn(referencedColumnName = "compositeKeyOne")
		@JoinColumn(referencedColumnName = "compositeKeyTwo")
		Set<This> theseOnCompositeKey = new HashSet<>();
	}

	@Entity(name = "This")
	static class This {
		@Id @GeneratedValue
		long id;
	}
}
