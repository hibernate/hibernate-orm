/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.constraint;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


@Jpa(annotatedClasses = {ConstraintInterpretationTest2.Enttity1.class, ConstraintInterpretationTest2.Entity2.class})
public class ConstraintInterpretationTest2 {
	@Test void testNotNullPrimaryKey(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			try {
				em.createNativeQuery( "insert into table_1 (id, name, ssn) values (null, 'test', 'abc123')" ).executeUpdate();
				fail();
			}
			catch (ConstraintViolationException cve) {
				assertEquals( ConstraintViolationException.ConstraintKind.NOT_NULL, cve.getKind() );
				// DB2 and Informix error messages don't contain the primary key constraint name
				if ( !(scope.getDialect() instanceof DB2Dialect) && !(scope.getDialect() instanceof InformixDialect) ) {
					assertTrue( cve.getConstraintName().toLowerCase().endsWith( "id" ) );
				}
			}
		} );
	}
	@Test void testUniquePrimaryKey(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			try {
				em.createNativeQuery( "insert into table_1 (id, name, ssn) values (1, 'test1', 'abc123')" ).executeUpdate();
				em.createNativeQuery( "insert into table_1 (id, name, ssn) values (1, 'test2', 'xyz456')" ).executeUpdate();
				fail();
			}
			catch (ConstraintViolationException cve) {
				assertEquals( ConstraintViolationException.ConstraintKind.UNIQUE, cve.getKind() );
			}
		} );
	}
	@Test void testNotNull(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			try {
				em.createNativeQuery( "insert into table_1 (id, name, ssn) values (1, null, 'abc123')" ).executeUpdate();
				fail();
			}
			catch (ConstraintViolationException cve) {
				assertEquals( ConstraintViolationException.ConstraintKind.NOT_NULL, cve.getKind() );
				// DB2 error message doesn't contain constraint or column name
				if ( !(scope.getDialect() instanceof DB2Dialect) ) {
					assertTrue( cve.getConstraintName().toLowerCase().endsWith( "name" ) );
				}
			}
		} );
	}
	@Test void testUnique(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			try {
				em.createNativeQuery( "insert into table_1 (id, name, ssn) values (1, 'test1', 'abc123')" ).executeUpdate();
				em.createNativeQuery( "insert into table_1 (id, name, ssn) values (2, 'test2', 'abc123')" ).executeUpdate();
				fail();
			}
			catch (ConstraintViolationException cve) {
				assertEquals( ConstraintViolationException.ConstraintKind.UNIQUE, cve.getKind() );
				// DB2 error message doesn't contain unique constraint name
				if ( !(scope.getDialect() instanceof DB2Dialect) ) {
					assertTrue( cve.getConstraintName().toLowerCase().contains( "ssnuk" ) );
				}
			}
		} );
	}

	@Test void testCheck(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			try {
				em.createNativeQuery( "insert into table_1 (id, name, ssn) values (1, ' ', 'abc123')" ).executeUpdate();
				fail();
			}
			catch (ConstraintViolationException cve) {
				assertEquals( ConstraintViolationException.ConstraintKind.CHECK, cve.getKind() );
				// CockroachDB error messages don't contain the check constraint name
				if ( !(scope.getDialect() instanceof CockroachDialect) ) {
					assertTrue( cve.getConstraintName().toLowerCase().endsWith( "namecheck" ) );
				}
			}
		} );
	}
	@Test void testForeignKey(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			try {
				em.createNativeQuery( "insert into table_2 (id, id1) values (1, 69)" ).executeUpdate();
				fail();
			}
			catch (ConstraintViolationException cve) {
				assertEquals( ConstraintViolationException.ConstraintKind.FOREIGN_KEY, cve.getKind() );
				// HANA error messages don't contain the foreign key constraint name
				if ( !(scope.getDialect() instanceof HANADialect) ) {
					assertTrue(  cve.getConstraintName().toLowerCase().endsWith( "id2to1fk" ) );
				}
			}
		} );
	}
	@Entity @Table(name = "table_1",
			uniqueConstraints = @UniqueConstraint(name = "ssnuk", columnNames = "ssn"),
			check = @CheckConstraint(name = "namecheck", constraint = "name <> ' '"))
	static class Enttity1 {
		@Id Long id;
		@Column(nullable = false)
		String name;
		@Column(nullable = false)
		String ssn;
	}
	@Entity @Table(name = "table_2")
	static class Entity2 {
		@Id Long id;
		@JoinColumn(name = "id1", foreignKey = @ForeignKey(name = "id2to1fk"))
		@ManyToOne Enttity1 other;
		@Column(unique = true)
		String whatever;
	}
}
