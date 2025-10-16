/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import org.hibernate.orm.test.envers.entities.StrTestEntity;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.envers.junit.EnversTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {StrTestEntity.class, ExceptionListenerRevEntity.class})
public class ExceptionListener {

	@Test
	public void testTransactionRollback(EntityManagerFactoryScope scope) {
		// Trying to persist an entity - however the listener should throw an exception, so the entity
		// shouldn't be persisted
		assertThrows( RuntimeException.class, () -> {
			scope.inTransaction( em -> {
				StrTestEntity te = new StrTestEntity( "x" );
				em.persist( te );
			} );
		} );
	}

	@Test
	public void testDataNotPersisted(EntityManagerFactoryScope scope) {
		// Checking if the entity became persisted
		scope.inTransaction( em -> {
			Long count = (Long) em.createQuery( "select count(s) from StrTestEntity s where s.str = 'x'" )
					.getSingleResult();
			assertEquals( 0L, count );
		} );
	}
}
