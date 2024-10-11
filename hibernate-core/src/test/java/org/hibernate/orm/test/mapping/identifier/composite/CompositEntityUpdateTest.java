/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier.composite;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = {
				CompositEntityUpdateTest.TimeScalePkOidDatum.class,
				CompositEntityUpdateTest.TimeScalePkOidDatumEmbed.class,
				CompositEntityUpdateTest.TestEntity.class,
				CompositEntityUpdateTest.TestEntityEmbed.class,
		}
)
@ServiceRegistry(
		settings = {
				// For your own convenience to see generated queries:
				@Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
				@Setting(name = AvailableSettings.FORMAT_SQL, value = "true"),
				// @Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
		}
)
@SessionFactory
@Jira("HHH-18721")
public class CompositEntityUpdateTest {

	@Test
	void hhh18721TestIdClass(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			TestEntity e = new TestEntity("yyy", "foo");
			em.persist(e);
			em.flush();
			assertTrue(em.contains(e));
			assertEquals(e.getData(), "foo");

			e.updateData("bar");
			em.flush();
			assertEquals(e.getData(), "bar");
		} );
	}

	@Test
	void hhh18721TestEmbedded(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			TestEntityEmbed e = new TestEntityEmbed("yyy", "foo");
			em.persist(e);
			em.flush();
			assertTrue(em.contains(e));
			assertEquals(e.getData(), "foo");

			e.updateData("bar");
			em.flush();
			assertEquals(e.getData(), "bar");
		} );
	}

	/**
	 * Composite primary key class for oid and datum
	 */
	public static class TimeScalePkOidDatum implements Serializable {

		@Serial
		private static final long serialVersionUID = 1L;

		private String oid;
		private Instant datum;

		public TimeScalePkOidDatum() {
			// Default constructor
		}

		public TimeScalePkOidDatum(String oid, Instant datum) {
			this.oid = oid;
			this.datum = datum;
		}

		public String oid() {
			return this.oid;
		}

		public Instant datum() {
			return this.datum;
		}
	}

	@Embeddable
	public static class TimeScalePkOidDatumEmbed implements Serializable {

		@Serial
		private static final long serialVersionUID = 1L;

		private String oid;
		private Instant datum;

		public TimeScalePkOidDatumEmbed() {
			// Default constructor
		}

		public TimeScalePkOidDatumEmbed(String oid, Instant datum) {
			this.oid = oid;
			this.datum = datum;
		}

		public String oid() {
			return this.oid;
		}

		public Instant datum() {
			return this.datum;
		}
	}

	@Entity
	@IdClass(TimeScalePkOidDatum.class)
	public static class TestEntity {

		@Id
		@Column(length = 32, nullable = false)
		private String oid = null;

		@Id
		@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE", nullable = false)
		private Instant datum;

		private String data;

		protected TestEntity() {
			// For JPA
		}

		public TestEntity(String oid, String data) {
			this.oid = oid;
			this.datum = Instant.now();
			this.data = data;
		}

		public Instant getDatum() {
			return datum;
		}

		public String getData() {
			return data;
		}

		public void updateData(String data) {
			this.data = data;
		}

		public String getOid() {
			return oid;
		}
	}

	@Entity
	public static class TestEntityEmbed {

		@Id
		@Embedded
		private TimeScalePkOidDatumEmbed id;

		private String data;

		protected TestEntityEmbed() {
			// For JPA
		}

		public TestEntityEmbed(String oid, String data) {
			this.id = new TimeScalePkOidDatumEmbed(oid, Instant.now());
			this.data = data;
		}

		public Instant getDatum() {
			return id.datum();
		}

		public String getData() {
			return data;
		}

		public void updateData(String data) {
			this.data = data;
		}

		public String getOid() {
			return id.oid();
		}
	}

}
