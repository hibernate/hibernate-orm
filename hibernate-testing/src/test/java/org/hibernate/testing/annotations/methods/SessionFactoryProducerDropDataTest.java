/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.annotations.methods;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.annotations.AnEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DropDataTiming;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SessionFactoryFunctionalTesting;
import org.hibernate.testing.orm.junit.SessionFactoryProducer;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SessionFactoryScopeParameterResolver;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for dropTestData functionality with SessionFactoryProducer.
 *
 * @author inpink
 */
public class SessionFactoryProducerDropDataTest {

	@Nested
	@SessionFactoryFunctionalTesting
	@ExtendWith(SessionFactoryScopeParameterResolver.class)
	@DomainModel(annotatedClasses = AnEntity.class)
	@ServiceRegistry(
			settings = {
					@Setting(name = "jakarta.persistence.schema-generation.database.action", value = "drop-and-create")
			}
	)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class DefaultDropDataTimingTests implements SessionFactoryProducer {

		@Override
		public SessionFactoryImplementor produceSessionFactory(MetadataImplementor model) {
			return (SessionFactoryImplementor) model.getSessionFactoryBuilder().build();
		}

		@Test
		public void testDefaultDropDataTimingIsNone(SessionFactoryScope scope) {
			DropDataTiming[] timings = this.dropTestData();
			assertThat(timings).isEmpty();
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

	@Nested
	@SessionFactoryFunctionalTesting
	@ExtendWith(SessionFactoryScopeParameterResolver.class)
	@DomainModel(annotatedClasses = AnEntity.class)
	@ServiceRegistry(
			settings = {
					@Setting(name = "jakarta.persistence.schema-generation.database.action", value = "drop-and-create")
			}
	)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class AfterEachDropDataTests implements SessionFactoryProducer {

		@Override
		public SessionFactoryImplementor produceSessionFactory(MetadataImplementor model) {
			return (SessionFactoryImplementor) model.getSessionFactoryBuilder().build();
		}

		@Override
		public DropDataTiming[] dropTestData() {
			return new DropDataTiming[]{ DropDataTiming.AFTER_EACH };
		}

		@Test
		@Order(1)
		public void insertDataWithAfterEachDrop(SessionFactoryScope scope) {
			scope.inTransaction(session -> {
				session.persist(new AnEntity(200, "After Each Producer"));
			});

			scope.inTransaction(session -> {
				Long count = session.createQuery("select count(e) from AnEntity e", Long.class)
						.getSingleResult();
				assertThat(count).isEqualTo(1L);
			});
		}

		@Test
		@Order(2)
		public void verifyAfterEachDropWorks(SessionFactoryScope scope) {
			scope.inTransaction(session -> {
				Long count = session.createQuery("select count(e) from AnEntity e", Long.class)
						.getSingleResult();
				assertThat(count).isEqualTo(0L);
			});
		}
	}

	@Nested
	@SessionFactoryFunctionalTesting
	@ExtendWith(SessionFactoryScopeParameterResolver.class)
	@DomainModel(annotatedClasses = AnEntity.class)
	@ServiceRegistry(
			settings = {
					@Setting(name = "jakarta.persistence.schema-generation.database.action", value = "drop-and-create")
			}
	)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class BeforeEachDropDataTests implements SessionFactoryProducer {

		@Override
		public SessionFactoryImplementor produceSessionFactory(MetadataImplementor model) {
			return (SessionFactoryImplementor) model.getSessionFactoryBuilder().build();
		}

		@Override
		public DropDataTiming[] dropTestData() {
			return new DropDataTiming[]{ DropDataTiming.BEFORE_EACH };
		}

		@Test
		@Order(1)
		public void prepareDataForBeforeEachDrop(SessionFactoryScope scope) {
			scope.inTransaction(session -> {
				session.persist(new AnEntity(300, "Before Each Producer"));
			});
		}

		@Test
		@Order(2)
		public void verifyBeforeEachDropWorks(SessionFactoryScope scope) {
			scope.inTransaction(session -> {
				Long count = session.createQuery("select count(e) from AnEntity e", Long.class)
						.getSingleResult();
				assertThat(count).isEqualTo(0L);
			});
		}
	}
}
