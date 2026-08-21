/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import jakarta.persistence.*;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.*;
import java.time.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DomainModel(annotatedClasses = JavaTimeMinMaxFunctionTest.Foo.class)
@ServiceRegistry(settings = {@Setting(name = AvailableSettings.JAVA_TIME_USE_DIRECT_JDBC, value = "true")})
@SessionFactory
@RequiresDialect(H2Dialect.class)
@JiraKey( value = "HHH-20302")
public class JavaTimeMinMaxFunctionTest {

	@Test
	void maxMinFunctionsShouldWorkWithLocalDateTimeAndDirectJdbc(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			var foo1 = new Foo();
			var earlier = LocalDateTime.of(2000, 1, 1, 10, 0, 0);
			foo1.ldtField = earlier;
			session.persist(foo1);

			var foo2 = new Foo();
			var later = LocalDateTime.of(2000, 12, 31, 23, 59, 59);
			foo2.ldtField = later;
			session.persist(foo2);

			LocalDateTime maxValue = session.createSelectionQuery(
					"select max(ldtField) from Foo",
					LocalDateTime.class
			).getSingleResult();

			assertEquals(
					later,
					maxValue,
					"MAX() on LocalDateTime should return the latest value even with JAVA_TIME_USE_DIRECT_JDBC=true"
			);

			LocalDateTime minValue = session.createSelectionQuery(
					"select min(ldtField) from Foo",
					LocalDateTime.class
			).getSingleResult();

			assertEquals(
					earlier,
					minValue,
					"MIN() on LocalDateTime should return the earliest value even with JAVA_TIME_USE_DIRECT_JDBC=true"
			);
		});
	}

	@Test
	void maxMinFunctionsShouldWorkWithLocalDateAndDirectJdbc(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			var foo1 = new Foo();
			var earlier = LocalDate.of(2000, 1, 1);
			foo1.ldField = earlier;
			session.persist(foo1);

			var foo2 = new Foo();
			var later = LocalDate.of(2000, 12, 31);
			foo2.ldField = later;
			session.persist(foo2);

			LocalDate maxValue = session.createSelectionQuery(
					"select max(ldField) from Foo",
					LocalDate.class
			).getSingleResult();

			assertEquals(
					later,
					maxValue,
					"MAX() on LocalDate should return the latest value even with JAVA_TIME_USE_DIRECT_JDBC=true"
			);

			LocalDate minValue = session.createSelectionQuery(
					"select min(ldField) from Foo",
					LocalDate.class
			).getSingleResult();

			assertEquals(
					earlier,
					minValue,
					"MIN() on LocalDate should return the earliest value even with JAVA_TIME_USE_DIRECT_JDBC=true"
			);
		});
	}

	@Test
	void maxMinFunctionsShouldWorkWithLocalTimeAndDirectJdbc(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			var foo1 = new Foo();
			var earlier = LocalTime.of(9, 0, 0);
			foo1.ltField = earlier;
			session.persist(foo1);

			var foo2 = new Foo();
			var later = LocalTime.of(17, 30, 0);
			foo2.ltField = later;
			session.persist(foo2);

			LocalTime maxValue = session.createSelectionQuery(
					"select max(ltField) from Foo",
					LocalTime.class
			).getSingleResult();

			assertEquals(
					later,
					maxValue,
					"MAX() on LocalTime should return the latest value even with JAVA_TIME_USE_DIRECT_JDBC=true"
			);

			LocalTime minValue = session.createSelectionQuery(
					"select min(ltField) from Foo",
					LocalTime.class
			).getSingleResult();

			assertEquals(
					earlier,
					minValue,
					"MIN() on LocalTime should return the earliest value even with JAVA_TIME_USE_DIRECT_JDBC=true"
			);
		});
	}

	@Test
	void maxMinFunctionsShouldWorkWithInstantAndDirectJdbc(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			var foo1 = new Foo();
			var earlier = LocalDateTime.of(2000, 1, 1, 0, 0, 0).toInstant(ZoneOffset.UTC);
			foo1.instantField = earlier;
			session.persist(foo1);

			var foo2 = new Foo();
			var later = LocalDateTime.of(2000, 12, 31, 23, 59, 59).toInstant(ZoneOffset.UTC);
			foo2.instantField = later;
			session.persist(foo2);

			Instant maxValue = session.createSelectionQuery(
					"select max(instantField) from Foo",
					Instant.class
			).getSingleResult();

			assertEquals(
					later,
					maxValue,
					"MAX() on Instant should return the latest value even with JAVA_TIME_USE_DIRECT_JDBC=true"
			);

			Instant minValue = session.createSelectionQuery(
					"select min(instantField) from Foo",
					Instant.class
			).getSingleResult();

			assertEquals(
					earlier,
					minValue,
					"MIN() on Instant should return the earliest value even with JAVA_TIME_USE_DIRECT_JDBC=true"
			);
		});
	}

	@Test
	void maxMinFunctionsShouldWorkWithOffsetDateTimeAndDirectJdbc(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			var foo1 = new Foo();
			var earlier = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
			foo1.odtField = earlier;
			session.persist(foo1);

			var foo2 = new Foo();
			var later = OffsetDateTime.of(2000, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
			foo2.odtField = later;
			session.persist(foo2);

			OffsetDateTime maxValue = session.createSelectionQuery(
					"select max(odtField) from Foo",
					OffsetDateTime.class
			).getSingleResult();

			assertEquals(
					later,
					maxValue,
					"MAX() on OffsetDateTime should return the latest value even with JAVA_TIME_USE_DIRECT_JDBC=true"
			);

			OffsetDateTime minValue = session.createSelectionQuery(
					"select min(odtField) from Foo",
					OffsetDateTime.class
			).getSingleResult();

			assertEquals(
					earlier,
					minValue,
					"MIN() on OffsetDateTime should return the earliest value even with JAVA_TIME_USE_DIRECT_JDBC=true"
			);
		});
	}

	@Test
	void maxMinFunctionsShouldWorkWithOffsetTimeAndDirectJdbc(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			var foo1 = new Foo();
			var earlier = OffsetTime.of(9, 0, 0, 0, ZoneOffset.UTC);
			foo1.otField = earlier;
			session.persist(foo1);

			var foo2 = new Foo();
			var later = OffsetTime.of(17, 30, 0, 0, ZoneOffset.UTC);
			foo2.otField = later;
			session.persist(foo2);

			OffsetTime maxValue = session.createSelectionQuery(
					"select max(otField) from Foo",
					OffsetTime.class
			).getSingleResult();

			assertEquals(
					later,
					maxValue,
					"MAX() on OffsetTime should return the latest value even with JAVA_TIME_USE_DIRECT_JDBC=true"
			);

			OffsetTime minValue = session.createSelectionQuery(
					"select min(otField) from Foo",
					OffsetTime.class
			).getSingleResult();

			assertEquals(
					earlier,
					minValue,
					"MIN() on OffsetTime should return the earliest value even with JAVA_TIME_USE_DIRECT_JDBC=true"
			);
		});
	}

	@Test
	void maxMinFunctionsShouldWorkWithZonedDateTimeAndDirectJdbc(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			var foo1 = new Foo();
			var earlier = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
			foo1.zdtField = earlier;
			session.persist(foo1);

			var foo2 = new Foo();
			var later = ZonedDateTime.of(2000, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC"));
			foo2.zdtField = later;
			session.persist(foo2);

			ZonedDateTime maxValue = session.createSelectionQuery(
					"select max(zdtField) from Foo", ZonedDateTime.class
			).getSingleResult();

			assertTrue(later.isEqual(maxValue),
					"MAX() on ZonedDateTime should return the latest value even with JAVA_TIME_USE_DIRECT_JDBC=true");

			ZonedDateTime minValue = session.createSelectionQuery(
					"select min(zdtField) from Foo", ZonedDateTime.class
			).getSingleResult();

			assertTrue(earlier.isEqual(minValue),
					"MIN() on ZonedDateTime should return the earliest value even with JAVA_TIME_USE_DIRECT_JDBC=true");
		});
	}

	@Entity(name = "Foo")
	public static class Foo{

		@Id
		@GeneratedValue
		Long id;

		LocalDateTime ldtField;

		LocalDate ldField;

		LocalTime ltField;

		Instant instantField;

		OffsetDateTime odtField;

		OffsetTime otField;

		ZonedDateTime zdtField;
	}
}
