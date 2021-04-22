/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.transaction;

import javax.persistence.EntityTransaction;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */
@Jpa
public class GetTransactionTest {

	@Test
	public void testMultipleCallsReturnTheSameTransaction(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					EntityTransaction t = entityManager.getTransaction();
					assertSame( t, entityManager.getTransaction() );
					assertFalse( t.isActive() );
					t.begin();
					assertSame( t, entityManager.getTransaction() );
					assertTrue( t.isActive() );
					t.commit();
					assertSame( t, entityManager.getTransaction() );
					assertFalse( t.isActive() );
					entityManager.close();
					assertSame( t, entityManager.getTransaction() );
					assertFalse( t.isActive() );
				}
		);
	}
}

