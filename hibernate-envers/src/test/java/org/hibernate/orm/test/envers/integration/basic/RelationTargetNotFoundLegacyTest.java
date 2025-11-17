/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test that when using the legacy default behavior, any {@code EntityNotFoundException} will
 * continue to be thrown, ala preserving pre 6.0 behavior.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-8051")
@EnversTest
@Jpa(annotatedClasses = {
		RelationTargetNotFoundLegacyTest.Foo.class,
		RelationTargetNotFoundLegacyTest.Bar.class,
		RelationTargetNotFoundLegacyTest.FooBar.class
})
public class RelationTargetNotFoundLegacyTest {

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1, initialize the data for test case
		scope.inTransaction( em -> {
			final Bar bar = new Bar( 1 );
			em.persist( bar );

			final FooBar fooBar1 = new FooBar( 1, "fooBar" );
			em.persist( fooBar1 );

			final FooBar fooBar2 = new FooBar( 2, "fooBar2" );
			em.persist( fooBar2 );

			final Foo foo = new Foo( 1, bar, fooBar1, fooBar2 );
			em.persist( foo );
		} );
	}

	@Test
	public void testRelationTargetNotFoundAction(EntityManagerFactoryScope scope) {
		// This test verifies that everything is fine before doing various record manipulation changes.
		scope.inEntityManager( em -> {
			final AuditReader auditReader = AuditReaderFactory.get( em );
			final Foo rev1 = auditReader.find( Foo.class, 1, 1 );
			assertNotNull( rev1 );
			assertNotNull( rev1.getBar() );
			assertNotNull( rev1.getFooBar() );
			assertNotNull( rev1.getFooBar2() );
		} );

		// Simulate the removal of main data table data by removing FooBar1 (an audited entity)
		scope.inTransaction( em -> {
			// obviously we assume either there isn't a FK between tables or the users do something like this
			em.createNativeQuery( "UPDATE Foo Set fooBar_id = NULL WHERE id = 1" ).executeUpdate();
			em.createNativeQuery( "DELETE FROM FooBar WHERE id = 1" ).executeUpdate();
		} );

		// This shouldn't fail because the audited entity data is cached in the audit table and exists.
		scope.inEntityManager( em -> {
			final AuditReader auditReader = AuditReaderFactory.get( em );
			final Foo rev1 = auditReader.find( Foo.class, 1, 1 );
			assertNotNull( rev1 );
			assertNotNull( Hibernate.unproxy( rev1.getFooBar() ) );
		} );

		// Simulate the removal of envers data via purge process by removing FooBar2 (an audited entity)
		scope.inTransaction( em -> {
			em.createNativeQuery( "DELETE FROM FooBar_AUD WHERE id = 2" ).executeUpdate();
		} );

		// Test querying history record where the reference audit row no longer exists.
		scope.inEntityManager( em -> {
			final AuditReader auditReader = AuditReaderFactory.get( em );
			final Foo rev1 = auditReader.find( Foo.class, 1, 1 );
			assertNotNull( rev1 );
			// With RelationTargetNotFoundAction.ERROR, this would throw an EntityNotFoundException.
			Exception exception = assertThrows( Exception.class, () -> Hibernate.unproxy( rev1.getFooBar2() ) );
			assertInstanceOf( EntityNotFoundException.class, exception );
		} );

		// this simulates the removal of a non-audited entity from the main table
		scope.inTransaction( em -> {
			// obviously we assume either there isn't a FK between tables or the users do something like this
			em.createNativeQuery( "UPDATE Foo SET bar_id = NULL WHERE id = 1" ).executeUpdate();
			em.createNativeQuery( "DELETE FROM Bar WHERE id = 1" ).executeUpdate();
		} );

		// Test querying history record where the reference non-audited row no longer exists.
		scope.inEntityManager( em -> {
			final AuditReader auditReader = AuditReaderFactory.get( em );
			final Foo rev1 = auditReader.find( Foo.class, 1, 1 );
			assertNotNull( rev1 );
			// With RelationTargetNotFoundAction.ERROR, this would throw an EntityNotFoundException
			Exception exception = assertThrows( Exception.class, () -> Hibernate.unproxy( rev1.getBar() ) );
			assertInstanceOf( EntityNotFoundException.class, exception );
		} );
	}

	@Audited
	@Entity(name = "Foo")
	public static class Foo {
		@Id
		private Integer id;

		@ManyToOne
		@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
		private Bar bar;

		@ManyToOne
		private FooBar fooBar;

		@ManyToOne
		private FooBar fooBar2;

		Foo() {
			// Required by JPA
		}

		Foo(Integer id, Bar bar, FooBar fooBar, FooBar fooBar2) {
			this.id = id;
			this.bar = bar;
			this.fooBar = fooBar;
			this.fooBar2 = fooBar2;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Bar getBar() {
			return bar;
		}

		public void setBar(Bar bar) {
			this.bar = bar;
		}

		public FooBar getFooBar() {
			return fooBar;
		}

		public void setFooBar(FooBar fooBar) {
			this.fooBar = fooBar;
		}

		public FooBar getFooBar2() {
			return fooBar2;
		}

		public void setFooBar2(FooBar fooBar2) {
			this.fooBar2 = fooBar2;
		}
	}

	@Entity(name = "Bar")
	public static class Bar {
		@Id
		private Integer id;

		Bar() {
			// Required by JPA
		}

		Bar(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Audited
	@Entity(name = "FooBar")
	public static class FooBar {
		@Id
		private Integer id;
		private String name;

		FooBar() {
			// Required by JPA
		}

		FooBar(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

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
}
