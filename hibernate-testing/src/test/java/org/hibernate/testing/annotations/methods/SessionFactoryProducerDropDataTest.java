/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.annotations.methods;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.annotations.AnEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SessionFactoryFunctionalTesting;
import org.hibernate.testing.orm.junit.SessionFactoryProducer;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SessionFactoryScopeParameterResolver;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that SessionFactoryProducer-only tests default to NEVER for drop timing.
 *
 * @author inpink
 */
@SessionFactoryFunctionalTesting
@ExtendWith(SessionFactoryScopeParameterResolver.class)
@DomainModel(annotatedClasses = AnEntity.class)
@ServiceRegistry(
		settings = {
				@Setting(name = "jakarta.persistence.schema-generation.database.action", value = "drop-and-create")
		}
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SessionFactoryProducerDropDataTest implements SessionFactoryProducer {

	@Override
	public SessionFactoryImplementor produceSessionFactory(MetadataImplementor model) {
		return (SessionFactoryImplementor) model.getSessionFactoryBuilder().build();
	}

	@Test
	@Order(1)
	public void insertData(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			session.persist(new AnEntity(100, "Producer Insert"));
		});
	}

	@Test
	@Order(2)
	public void dataRemainsWithoutExplicitDrop(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			Long count = session.createQuery("select count(e) from AnEntity e", Long.class)
					.getSingleResult();
			assertThat(count).isEqualTo(1L);
		});

		scope.dropData();
	}
}
