/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ops;

import jakarta.persistence.OptimisticLockException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Emmanuel Bernard
 */
@Jpa(annotatedClasses = {
		Workload.class
})
public class MergeNewTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testMergeNew(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						Workload load = new Workload();
						load.name = "Cleaning";
						load.load = 10;
						entityManager.getTransaction().begin();
						load = entityManager.merge( load );
						assertNotNull( load.id );
						entityManager.flush();
						assertNotNull( load.id );
					}
					finally {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
					}
				}
		);
	}

	@Test
	public void testMergeAfterRemove(EntityManagerFactoryScope scope) {

		Integer load_id = scope.fromTransaction(
				entityManager -> {
					Workload _load = new Workload();
					_load.name = "Cleaning";
					_load.load = 10;
					_load = entityManager.merge( _load );
					entityManager.flush();
					return _load.getId();
				}
		);

		Workload load =scope.fromTransaction(
				entityManager -> {
					Workload _load = entityManager.find( Workload.class, load_id );
					entityManager.remove( _load );
					entityManager.flush();
					return _load;
				}
		);

		scope.inTransaction(
				entityManager -> {
					try {
						entityManager.merge( load );
						entityManager.flush();
					}
					catch (OptimisticLockException e) {
						//expected since object can be inferred detached
						assertTrue( e.getCause() instanceof StaleObjectStateException );
					}
				}
		);
	}
}
