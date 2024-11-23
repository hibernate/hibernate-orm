/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.factory.puUtil;


import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {
		ModernEntity.class
})
public class GetIdentifierTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from ModernEntity" ).executeUpdate();
				}
		);
	}

	@Test
	public void getIdentifierTest(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					ModernEntity modernEntity = new ModernEntity();
					modernEntity.setFoo( 2 );

					Object simpleEntityId = entityManager.getEntityManagerFactory()
							.getPersistenceUnitUtil().getIdentifier( modernEntity );
				}
		);
	}

	@Test
	public void getIdentifierOfNonEntityTest(EntityManagerFactoryScope scope) {
		try {
			scope.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier( this );
			fail( "should have thrown IllegalArgumentException" );
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	@Test
	public void getIdentifierOfNullTest(EntityManagerFactoryScope scope) {
		try {
			scope.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier( null );
			fail( "should have thrown IllegalArgumentException" );
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}
}
