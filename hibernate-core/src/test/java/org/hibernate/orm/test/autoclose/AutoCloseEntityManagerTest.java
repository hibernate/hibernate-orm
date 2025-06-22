/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.autoclose;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa
class AutoCloseEntityManagerTest {
	@Test void testAutoClose(EntityManagerFactoryScope scope) {
		SessionFactory factory = scope.getEntityManagerFactory().unwrap( SessionFactory.class );
		EntityManager session = factory.withOptions().autoClose( true ).openSession();
		var tx = session.getTransaction();
		tx.begin();
		tx.commit();
		assertFalse( session.isOpen() );
	}

	@Test void testNoAutoClose(EntityManagerFactoryScope scope) {
		SessionFactory factory = scope.getEntityManagerFactory().unwrap( SessionFactory.class );
		EntityManager session = factory.withOptions().autoClose( false ).openSession();
		var tx = session.getTransaction();
		tx.begin();
		tx.commit();
		assertTrue( session.isOpen() );
		session.close();
	}

	@Test void testCloseWithTx(EntityManagerFactoryScope scope) {
		EntityManager session = scope.getEntityManagerFactory().createEntityManager();
		var tx = session.getTransaction();
		tx.begin();
		// this is tolerated in JPA bootstrap
		session.close();
		tx.commit();
		assertFalse( session.isOpen() );
	}
}
