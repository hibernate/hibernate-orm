/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import org.hibernate.query.SemanticException;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Query;

import static org.assertj.core.api.Assertions.assertThat;

@Jpa(annotatedClasses = {
		EntityWithCompositeIdFkAssociation.class,
		EntityWithCompositeId.class,
		CompositeId.class
})
public class CompositeIdFkUpdateTest {

	@Test
	public void testUpdateAssociationSetNull(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Query q = entityManager.createQuery(
							"update EntityWithCompositeIdFkAssociation e set e.association = null" );

					q.executeUpdate();
				}
		);
	}

	@Test
	public void testUpdateAssociationSetRubbish(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					try {
						entityManager.createQuery(
								"update EntityWithCompositeIdFkAssociation e set e.association = 1" );
						Assertions.fail( "Expected query type validation to fail due to illegal assignment" );
					}
					catch (IllegalArgumentException ex) {
						assertThat( ex.getCause() ).isInstanceOf( SemanticException.class );
						assertThat( ex.getCause() ).hasMessageContaining( "Cannot assign expression of type" );
					}
				}
		);
	}

	@Test
	public void testUpdateAssociationSetAssociationPart(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Query q = entityManager.createQuery(
							"update EntityWithCompositeIdFkAssociation e set e.association.id.id1 = 1" );

					q.executeUpdate();
				}
		);
	}
}
