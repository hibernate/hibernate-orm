/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.emops;


import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 */
@Jpa(annotatedClasses = {
		Race.class,
		Competitor.class
})
public class RefreshTest {
	@Test
	public void testRefreshNonManaged(EntityManagerFactoryScope scope) throws Exception {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();
						Race race = new Race();
						entityManager.persist( race );
						entityManager.flush();
						entityManager.clear();

						try {
							entityManager.refresh( race );
							fail( "Refresh should fail on a non managed entity" );
						}
						catch (IllegalArgumentException e) {
							//success
						}

						entityManager.getTransaction().rollback();
					}
					catch (Exception e) {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}
}
