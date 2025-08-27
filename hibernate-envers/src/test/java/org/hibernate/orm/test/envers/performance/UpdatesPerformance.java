/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.performance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.entities.StrTestEntity;

import org.junit.Ignore;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Ignore
public class UpdatesPerformance extends AbstractPerformanceTest {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class};
	}

	private final static int NUMBER_UPDATES = 5000;
	private final static int NUMBER_ENTITIES = 10;

	private Random random = new Random();

	private List<Integer> ids = new ArrayList<Integer>();

	private void setup() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();

		for ( int i = 0; i < NUMBER_ENTITIES; i++ ) {
			StrTestEntity testEntity = new StrTestEntity( "x" + i );
			entityManager.persist( testEntity );
			ids.add( testEntity.getId() );
		}
		entityManager.getTransaction().commit();
	}

	protected void doTest() {
		setup();

		for ( int i = 0; i < NUMBER_UPDATES; i++ ) {
			newEntityManager();
			EntityManager entityManager = getEntityManager();

			entityManager.getTransaction().begin();
			Integer id = ids.get( random.nextInt( NUMBER_ENTITIES ) );
			start();
			StrTestEntity testEntity = entityManager.find( StrTestEntity.class, id );
			testEntity.setStr( "z" + i );
			entityManager.getTransaction().commit();
			stop();
		}
	}

	public static void main(String[] args) throws IOException {
		UpdatesPerformance updatesPerformance = new UpdatesPerformance();
		updatesPerformance.test( 3 );
	}
}
