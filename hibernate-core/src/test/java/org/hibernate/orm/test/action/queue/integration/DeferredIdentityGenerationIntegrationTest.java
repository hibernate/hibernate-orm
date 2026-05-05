/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.integration;

import org.hibernate.cfg.FlushSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies the opt-in graph queue mode that plans IDENTITY inserts with the
 * rest of the flush instead of executing them during persist().
 *
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {
		IdentityGenerationIntegrationTest.IdentityEntity.class,
		IdentityGenerationIntegrationTest.IdentityParent.class,
		IdentityGenerationIntegrationTest.IdentityChild.class
})
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "graph"),
		@Setting(name = FlushSettings.GRAPH_DEFER_IDENTITY_INSERTS, value = "true")
})
public class DeferredIdentityGenerationIntegrationTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			session.createMutationQuery("delete from IdentityChild").executeUpdate();
			session.createMutationQuery("delete from IdentityParent").executeUpdate();
			session.createMutationQuery("delete from IdentityEntity").executeUpdate();
		});
	}

	@Test
	public void testSimpleIdentityInsertIsDeferredUntilFlush(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			final IdentityGenerationIntegrationTest.IdentityEntity entity =
					new IdentityGenerationIntegrationTest.IdentityEntity();
			entity.setName("Test");

			session.persist(entity);

			assertNull(entity.getId(), "IDENTITY id should not be assigned before flush when graph deferral is enabled");

			session.flush();

			assertNotNull(entity.getId(), "IDENTITY id should be assigned after the deferred insert executes");
		});

		scope.inTransaction(session -> {
			assertEquals(1L, session.createQuery(
					"select count(*) from IdentityEntity",
					Long.class
			).getSingleResult());
		});
	}

	@Test
	public void testDeferredIdentityForeignKeyUsesGeneratedIdentifierHandle(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			final IdentityGenerationIntegrationTest.IdentityParent parent =
					new IdentityGenerationIntegrationTest.IdentityParent();
			parent.setName("Parent");
			session.persist(parent);

			final IdentityGenerationIntegrationTest.IdentityChild child =
					new IdentityGenerationIntegrationTest.IdentityChild();
			child.setName("Child");
			child.setParent(parent);
			session.persist(child);

			assertNull(parent.getId(), "Parent id should not be assigned before flush when graph deferral is enabled");
			assertNull(child.getId(), "Child id should not be assigned before flush when graph deferral is enabled");

			session.flush();

			assertNotNull(parent.getId(), "Parent id should be assigned after the deferred insert executes");
			assertNotNull(child.getId(), "Child id should be assigned after the deferred insert executes");
		});

		scope.inTransaction(session -> {
			final var parent = session.createQuery(
					"from IdentityParent",
					IdentityGenerationIntegrationTest.IdentityParent.class
			).getSingleResult();
			assertEquals(1, parent.getChildren().size());
			assertEquals("Child", parent.getChildren().get(0).getName());
		});
	}
}
