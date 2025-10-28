/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Version;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("JUnitMalformedDeclaration")
@Jpa(annotatedClasses = CurrentTimestampVersionTest.Timestamped.class)
class CurrentTimestampVersionTest {
	@AfterEach
	void tearDown(EntityManagerFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test void test(EntityManagerFactoryScope scope) {
		Timestamped t = scope.fromTransaction( entityManager -> {
			Timestamped timestamped = new Timestamped();
			timestamped.id = 1L;
			entityManager.persist( timestamped );
			return timestamped;
		} );
		assertNotNull( t.timestamp );
		scope.inTransaction( entityManager -> {
			Timestamped timestamped = entityManager.find( Timestamped.class, 1L,
					LockModeType.OPTIMISTIC_FORCE_INCREMENT );
			assertNotNull( timestamped );
			assertNotNull( timestamped.timestamp );
		} );
		scope.inTransaction( entityManager -> {
			Timestamped timestamped = entityManager.find( Timestamped.class, 1L );
			timestamped.content = "new content";
		} );
		// TODO: assert some stuff about the timestamp values
	}

	@Entity
	static class Timestamped {
		@Id
		Long id;
		@CurrentTimestamp @Version
		LocalDateTime timestamp;
		String content;
	}
}
