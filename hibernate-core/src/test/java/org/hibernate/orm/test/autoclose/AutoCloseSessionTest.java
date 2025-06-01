/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.autoclose;

import org.hibernate.Session;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel
class AutoCloseSessionTest {
	@Test void testAutoClose(SessionFactoryScope scope) {
		org.hibernate.SessionFactory factory = scope.getSessionFactory();
		Session session = factory.withOptions().autoClose( true ).openSession();
		var tx = session.beginTransaction();
		tx.commit();
		assertFalse( session.isOpen() );
	}

	@Test void testNoAutoClose(SessionFactoryScope scope) {
		org.hibernate.SessionFactory factory = scope.getSessionFactory();
		Session session = factory.withOptions().autoClose( false ).openSession();
		var tx = session.beginTransaction();
		tx.commit();
		assertTrue( session.isOpen() );
		session.close();
	}

	@Test void testCloseWithTx(SessionFactoryScope scope) {
		Session session = scope.getSessionFactory().openSession();
		var tx = session.beginTransaction();
		session.close();
		assertThrows( IllegalStateException.class, tx::commit );
		assertFalse( session.isOpen() );
	}
}
