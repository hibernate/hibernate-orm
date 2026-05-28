/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pc;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.ReadOnlyMode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
@DomainModel(annotatedClasses = EntityHandlerOptionTests.TestEntity.class)
@SessionFactory
public class EntityHandlerOptionTests {
	@Test
	void testReadOnlyMode(SessionFactoryScope factoryScope) {
		var sf = factoryScope.getSessionFactory();

		try (var em = sf.createEntityManager()) {
			assertThat(em.isDefaultReadOnly()).isFalse();
		}

		try (var em = sf.createEntityManager( ReadOnlyMode.READ_ONLY )) {
			assertThat(em.isDefaultReadOnly()).isTrue();
		}

		try (var em = sf.createEntityManager()) {
			assertThat(em.isDefaultReadOnly()).isFalse();
			em.addOption( ReadOnlyMode.READ_ONLY );
			assertThat(em.isDefaultReadOnly()).isTrue();
		}
	}

	@Entity
	public static class TestEntity {
		@Id
		private Long id;
		private String name;
	}
}
