/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.performance;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.hibernate.envers.test.entities.StrTestEntity;

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
