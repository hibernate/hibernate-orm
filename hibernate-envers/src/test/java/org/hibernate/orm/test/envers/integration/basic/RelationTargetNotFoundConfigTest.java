/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test that when the {@link EnversSettings#GLOBAL_RELATION_NOT_FOUND_LEGACY_FLAG} is {@code false}
 * that the ignore behavior is used by default rather than throwing {@code EntityNotFoundException}.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-8051")
public class RelationTargetNotFoundConfigTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Foo.class, Bar.class, FooBar.class };
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( EnversSettings.GLOBAL_RELATION_NOT_FOUND_LEGACY_FLAG, Boolean.FALSE );
	}


	@Test
	public void testRelationTargetNotFoundAction() {
		// Revision 1, initialize the data for test case
		doInJPA( this::entityManagerFactory, entityManager -> {
			final Bar bar = new Bar( 1 );
			entityManager.persist( bar );

			final FooBar fooBar1 = new FooBar( 1, "fooBar" );
			entityManager.persist( fooBar1 );

			final FooBar fooBar2 = new FooBar( 2, "fooBar2" );
			entityManager.persist( fooBar2 );

			final Foo foo = new Foo( 1, bar, fooBar1, fooBar2 );
			entityManager.persist( foo );
		} );

		// This test verifies that everything is fine before doing various record manipulation changes.
		doInJPA( this::entityManagerFactory, entityManager -> {
			final AuditReader auditReader = AuditReaderFactory.get(entityManager );
			final Foo rev1 = auditReader.find( Foo.class, 1, 1 );
			assertNotNull( rev1 );
			assertNotNull( rev1.getBar() );
			assertNotNull( rev1.getFooBar() );
			assertNotNull( rev1.getFooBar2() );
		} );

		// Simulate the removal of main data table data by removing FooBar1 (an audited entity)
		doInJPA( this::entityManagerFactory, entityManager -> {
			// obviously we assume either there isn't a FK between tables or the users do something like this
			entityManager.createNativeQuery( "UPDATE Foo Set fooBar_id = NULL WHERE id = 1" ).executeUpdate();
			entityManager.createNativeQuery( "DELETE FROM FooBar WHERE id = 1" ).executeUpdate();
		} );

		// This shouldn't fail because the audited entity data is cached in the audit table and exists.
		doInJPA( this::entityManagerFactory, entityManager -> {
			final AuditReader auditReader = AuditReaderFactory.get( entityManager );
			final Foo rev1 = auditReader.find( Foo.class, 1, 1 );
			assertNotNull( rev1 );
			assertNotNull( rev1.getFooBar() );
		} );

		// Simulate the removal of envers data via purge process by removing FooBar2 (an audited entity)
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.createNativeQuery( "DELETE FROM FooBar_AUD WHERE id = 2" ).executeUpdate();
		} );

		// Test querying history record where the reference audit row no longer exists.
		doInJPA( this::entityManagerFactory, entityManager -> {
			final AuditReader auditReader = AuditReaderFactory.get( entityManager );
			final Foo rev1 = auditReader.find( Foo.class, 1, 1 );
			assertNotNull( rev1 );
			// With RelationTargetNotFoundAction.ERROR, this would throw an EntityNotFoundException.
			assertNull( rev1.getFooBar2() );
		} );

		// this simulates the removal of a non-audited entity from the main table
		doInJPA( this::entityManagerFactory, entityManager -> {
			// obviously we assume either there isn't a FK between tables or the users do something like this
			entityManager.createNativeQuery( "UPDATE Foo SET bar_id = NULL WHERE id = 1" ).executeUpdate();
			entityManager.createNativeQuery( "DELETE FROM Bar WHERE id = 1" ).executeUpdate();
		} );

		// Test querying history record where the reference non-audited row no longer exists.
		doInJPA( this::entityManagerFactory, entityManager -> {
			final AuditReader auditReader = AuditReaderFactory.get( entityManager );
			final Foo rev1 = auditReader.find( Foo.class, 1, 1 );
			assertNotNull( rev1 );
			// With RelationTargetNotFoundAction.ERROR, this would throw an EntityNotFoundException
			assertNull( rev1.getBar() );
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
