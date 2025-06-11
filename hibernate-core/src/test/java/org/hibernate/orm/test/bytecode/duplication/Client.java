/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.duplication;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * To be loaded reflectively...
 *
 * @author Steve Ebersole
 */
public class Client {
	public static void execute(EntityManagerFactory emf) {
		emf.runInTransaction( (entityManager) -> {
			entityManager.persist( new SimpleEntity( 1, "tester" ) );
			entityManager.createQuery( "from SimpleEntity" ).getResultList();
		} );
	}

	public static void cleanup(EntityManagerFactory emf) {
		emf.unwrap( SessionFactoryImplementor.class ).getSchemaManager().truncateMappedObjects();
	}
}
