/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.data;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;


/**
 * Tests that {@link jakarta.persistence.Embedded} works correctly when combined with Java record classes
 * in the context of Envers auditing.
 *
 * @author Minjae Seon
 */
@JiraKey(value = "HHH-18691")
@EnversTest
@Jpa(annotatedClasses = {
		RecordFieldEntityTest.TestRecord.class,
		RecordFieldEntityTest.WithRecord.class,
		RecordFieldEntityTest.TestEmbeddedClass.class,
		RecordFieldEntityTest.WithoutRecord.class
})
public class RecordFieldEntityTest {
	record TestRecord(String foo, String bar) {
	}

	static class TestEmbeddedClass {
		public TestEmbeddedClass() {
		}

		public TestEmbeddedClass(String foo, String bar) {
			this.foo = foo;
			this.bar = bar;
		}

		private String foo;
		private String bar;

		public String getFoo() {
			return foo;
		}

		public String getBar() {
			return bar;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}
	}

	@Entity
	@Audited
	static class WithRecord {
		@Id
		private Integer id;

		@Embedded
		private TestRecord testRecord;

		public Integer getId() {
			return id;
		}

		static WithRecord of(int id, String foo, String bar) {
			WithRecord withRecord = new WithRecord();

			withRecord.id = id;
			withRecord.testRecord = new TestRecord( foo, bar );
			return withRecord;
		}
	}

	@Entity
	@Audited
	static class WithoutRecord {
		@Id
		private Integer id;

		@Embedded
		private TestEmbeddedClass testEmbeddedClass;

		public Integer getId() {
			return id;
		}

		public TestEmbeddedClass getTestEmbeddedClass() {
			return testEmbeddedClass;
		}

		static WithoutRecord of(int id, String foo, String bar) {
			WithoutRecord withoutRecord = new WithoutRecord();

			withoutRecord.id = id;
			withoutRecord.testEmbeddedClass = new TestEmbeddedClass( foo, bar );

			return withoutRecord;
		}
	}

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			// Create WithRecord Entity
			WithRecord withRecord = WithRecord.of( 1, "foo", "bar" );
			em.persist( withRecord );
			// Create WithoutRecord
			WithoutRecord withoutRecord = WithoutRecord.of( 1, "foo", "bar" );
			em.persist( withoutRecord );
		} );
	}

	@Test
	public void testLoadRecordData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			WithRecord recordRev = auditReader.find( WithRecord.class, 1, 1 );
			assertEquals( "WithRecord.TestRecord.foo equals foo", "foo", recordRev.testRecord.foo() );
			assertEquals( "WithRecord.TestRecord.bar equals bar", "bar", recordRev.testRecord.bar() );
		} );
	}

	@Test
	public void testLoadWithoutRecordData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			WithoutRecord withoutRecordRev = auditReader.find( WithoutRecord.class, 1, 1 );
			assertEquals( "WithoutRecord.TestEmbeddedClass.foo equals foo", "foo",
					withoutRecordRev.getTestEmbeddedClass().getFoo() );
			assertEquals( "WithoutRecord.TestEmbeddedClass.bar equals bar", "bar",
					withoutRecordRev.getTestEmbeddedClass().getBar() );
		} );
	}
}
