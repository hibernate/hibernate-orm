/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@DomainModel(annotatedClasses = TemporalParameterPlusDurationTest.SimpleEntity.class)
@SessionFactory
@JiraKey("HHH-18201")
public class TemporalParameterPlusDurationTest {

	@Test
	void timestampVsTimestampParameterPlusDuration(SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.createQuery( "from SimpleEntity where inst > :i + 1 second + 2 second", SimpleEntity.class )
					.setParameter( "i", Instant.now() )
					.getResultList();
		} );
	}

	@Test
	void timestampParameterPlusDurationVsTimestamp(SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.createQuery( "from SimpleEntity where :i + 1 second + 2 second > inst", SimpleEntity.class )
					.setParameter( "i", Instant.now() )
					.getResultList();
		} );
	}

	@Test
	void dateVsDateParameterPlusDuration(SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.createQuery( "from SimpleEntity where ldate > :i + 3 day + 2 day", SimpleEntity.class )
					.setParameter( "i", LocalDate.now() )
					.getResultList();
		} );
	}

	@Test
	void dateParameterPlusDurationVsDate(SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.createQuery( "from SimpleEntity where :i + 3 day + 2 day > ldate", SimpleEntity.class )
					.setParameter( "i", LocalDate.now() )
					.getResultList();
		} );
	}

	@Test
	void durationVsDurationParameterPlusDuration(SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.createQuery( "from SimpleEntity where dur > :i + 1 second", SimpleEntity.class )
					.setParameter( "i", Duration.ofMinutes( 1 ) )
					.getResultList();
		} );
	}

	@Test
	void durationParameterVsDurationPlusDuration(SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.createQuery( "from SimpleEntity where :i + 1 second > dur", SimpleEntity.class )
					.setParameter( "i", Duration.ofMinutes( 1 ) )
					.getResultList();
		} );
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		Integer id;

		Instant inst;

		LocalDate ldate;

		Duration dur;
	}
}
