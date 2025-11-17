/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel(annotatedClasses = PrePersistIdTest.GeneratedIdInCallback.class)
public class PrePersistIdTest {
	@Test void test(SessionFactoryScope scope) {
		scope.inTransaction(s -> {
			GeneratedIdInCallback entity = new GeneratedIdInCallback();
			s.persist(entity);
			assertTrue(entity.success);
			assertNotNull(entity.uuid);
		});
	}

	@Entity(name = "GeneratedIdInCallback")
	static class GeneratedIdInCallback {
		@Transient boolean success;
		@Id @GeneratedValue UUID uuid;
		@PrePersist void checkId() {
			success = uuid != null;
			assertNotNull(uuid);
		}
	}
}
