/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgresPlusDialect;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Jpa(annotatedClasses = NativeQueryWithDatetimesTest.Datetimes.class)
public class NativeQueryWithDatetimesTest {
	@SkipForDialect(dialectClass = PostgresPlusDialect.class)
	@SkipForDialect(dialectClass = OracleDialect.class)
	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction(s -> s.persist(new Datetimes()));
		Object[] result = scope.fromTransaction(s -> (Object[]) s.createNativeQuery("select ctime, cdate, cdatetime from tdatetimes", Object[].class).getSingleResult());
		assertInstanceOf(LocalTime.class, result[0]);
		assertInstanceOf(LocalDate.class, result[1]);
		assertInstanceOf(LocalDateTime.class, result[2]);
//		result = scope.fromTransaction(s -> (Object[]) s.createNativeQuery("select current_time, current_date, current_timestamp from tdatetimes", Object[].class).getSingleResult());
//		assertInstanceOf(LocalTime.class, result[0]);
//		assertInstanceOf(LocalDate.class, result[1]);
//		assertInstanceOf(LocalDateTime.class, result[2]);
	}

	@Entity @Table(name = "tdatetimes")
	static class Datetimes {
		@Id
		long id;
		@Column(nullable = false, name = "ctime")
		LocalTime localTime = LocalTime.now();
		@Column(nullable = false, name = "cdate")
		LocalDate localDate = LocalDate.now();
		@Column(nullable = false, name = "cdatetime")
		LocalDateTime localDateTime = LocalDateTime.now();
	}
}
