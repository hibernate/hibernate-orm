/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.performance;

import java.io.IOException;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.entities.StrTestEntity;

import org.junit.Ignore;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Ignore
public class InsertsPerformance extends AbstractPerformanceTest {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class};
	}

	private final static int NUMBER_INSERTS = 5000;

	protected void doTest() {
		for ( int i = 0; i < NUMBER_INSERTS; i++ ) {
			newEntityManager();
			EntityManager entityManager = getEntityManager();

			entityManager.getTransaction().begin();
			start();
			entityManager.persist( new StrTestEntity( "x" + i ) );
			entityManager.getTransaction().commit();
			stop();
		}
	}

	public static void main(String[] args) throws IOException {
		InsertsPerformance insertsPerformance = new InsertsPerformance();
		insertsPerformance.test( 3 );
	}
}
