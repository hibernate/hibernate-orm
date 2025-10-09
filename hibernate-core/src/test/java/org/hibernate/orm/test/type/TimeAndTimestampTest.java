/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-10465")
@SkipForDialect(dialectClass = MariaDBDialect.class)
@SkipForDialect(dialectClass = MySQLDialect.class)
@SkipForDialect(dialectClass = OracleDialect.class, reason = "Oracle date does not support milliseconds  ")
@SkipForDialect(dialectClass = HANADialect.class, reason = "HANA date does not support milliseconds  ")
@DomainModel(annotatedClasses = TimeAndTimestampTest.Event.class)
@SessionFactory
public class TimeAndTimestampTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void test(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			Event event = new Event();
			event.id = 1L;
			event.timeValue = new Time( 1000 );
			event.timestampValue = new Timestamp( 45677 );

			session.persist( event );
		} );

		factoryScope.inTransaction( (session) -> {
			Event event = session.find( Event.class, 1L );
			assertEquals(1000, event.timeValue.getTime() % TimeUnit.DAYS.toMillis( 1 ));
			assertEquals(45677, event.timestampValue.getTime() % TimeUnit.DAYS.toMillis( 1 ));
		} );
	}

	@Entity(name = "Event")
	public static class Event {
		@Id
		private Long id;
		private Time timeValue;
		private Timestamp timestampValue;
	}
}
