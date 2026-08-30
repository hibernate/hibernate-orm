/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.annotations.methods;

import org.hibernate.testing.annotations.AnEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DropDataTiming;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for dropTestData timing configuration.
 *
 * @author inpink
 */
public class DropDataTimingTest {

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	@DomainModel(annotatedClasses = AnEntity.class)
	@SessionFactory(dropTestData = {DropDataTiming.AFTER_EACH})
	class AfterEachDropDataTests {

		@Test
		@Order(1)
		public void testAfterEachDropData(SessionFactoryScope scope) {
			scope.inTransaction(session -> {
				AnEntity entity = new AnEntity(1, "After Each");
				session.persist(entity);
			});

			scope.inTransaction(session -> {
				Long count = session.createQuery("select count(e) from AnEntity e", Long.class)
						.getSingleResult();
				assertThat(count).isEqualTo(1L);
			});
		}

		@Test
		@Order(2)
		public void verifyAfterEachDropDataCleanup(SessionFactoryScope scope) {
			scope.inTransaction(session -> {
				Long count = session.createQuery("select count(e) from AnEntity e", Long.class)
						.getSingleResult();
				assertThat(count).isEqualTo(0L);
			});
		}
	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	@DomainModel(annotatedClasses = AnEntity.class)
	@SessionFactory(dropTestData = {DropDataTiming.BEFORE_EACH})
	class BeforeEachDropDataTests {

		@Test
		@Order(1)
		public void prepareBeforeEachDropData(SessionFactoryScope scope) {
			scope.inTransaction(session -> {
				AnEntity entity = new AnEntity(2, "Before Each");
				session.persist(entity);
			});
		}

		@Test
		@Order(2)
		public void testBeforeEachDropData(SessionFactoryScope scope) {
			scope.inTransaction(session -> {
				Long count = session.createQuery("select count(e) from AnEntity e", Long.class)
						.getSingleResult();
				assertThat(count).isEqualTo(0L);
			});
		}
	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	@DomainModel(annotatedClasses = AnEntity.class)
	@SessionFactory
	class NeverDropDataTests {

		@Test
		@Order(1)
		public void prepareNeverDropData(SessionFactoryScope scope) {
			scope.inTransaction(session -> {
				AnEntity entity = new AnEntity(3, "Never");
				session.persist(entity);
			});
		}

		@Test
		@Order(2)
		public void testNeverDropData(SessionFactoryScope scope) {
			scope.inTransaction(session -> {
				Long count = session.createQuery("select count(e) from AnEntity e", Long.class)
						.getSingleResult();
				assertThat(count).isGreaterThan(0L);
			});

		}
	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	@DomainModel(annotatedClasses = AnEntity.class)
	class MethodLevelDropDataTests {

		@Test
		@Order(1)
		@SessionFactory(dropTestData = {DropDataTiming.AFTER_EACH})
		public void methodLevelAfterEach(SessionFactoryScope scope) {
			scope.inTransaction(session -> {
				AnEntity entity = new AnEntity(10, "Method After Each");
				session.persist(entity);
			});

			scope.inTransaction(session -> {
				Long count = session.createQuery("select count(e) from AnEntity e", Long.class)
						.getSingleResult();
				assertThat(count).isEqualTo(1L);
			});
		}

		@Test
		@Order(2)
		@SessionFactory(dropTestData = {DropDataTiming.AFTER_EACH})
		public void methodLevelAfterEachCleanup(SessionFactoryScope scope) {
			scope.inTransaction(session -> {
				Long count = session.createQuery("select count(e) from AnEntity e", Long.class)
						.getSingleResult();
				assertThat(count).isEqualTo(0L);
			});
		}

		@Test
		@Order(3)
		@SessionFactory(dropTestData = {DropDataTiming.BEFORE_EACH})
		public void methodLevelBeforeEachInsert(SessionFactoryScope scope) {
			scope.inTransaction(session -> {
				AnEntity entity = new AnEntity(11, "Method Before Each");
				session.persist(entity);
			});
		}

		@Test
		@Order(4)
		@SessionFactory(dropTestData = {DropDataTiming.BEFORE_EACH})
		public void methodLevelBeforeEachVerify(SessionFactoryScope scope) {
			scope.inTransaction(session -> {
				Long count = session.createQuery("select count(e) from AnEntity e", Long.class)
						.getSingleResult();
				assertThat(count).isEqualTo(0L);
			});
		}

		@Test
		@Order(5)
		@SessionFactory
		public void methodLevelNeverInsert(SessionFactoryScope scope) {
			scope.inTransaction(session -> {
				AnEntity entity = new AnEntity(12, "Method Never");
				session.persist(entity);
			});
		}

		@Test
		@Order(6)
		@SessionFactory
		public void methodLevelNeverVerify(SessionFactoryScope scope) {
			scope.inTransaction(session -> {
				Long count = session.createQuery("select count(e) from AnEntity e", Long.class)
						.getSingleResult();
				assertThat(count).isEqualTo(0L);
			});

			scope.dropData();
		}
	}
}
