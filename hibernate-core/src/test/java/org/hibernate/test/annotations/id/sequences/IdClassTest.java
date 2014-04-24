/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.test.annotations.id.sequences;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.id.sequences.entities.Location;
import org.hibernate.test.annotations.id.sequences.entities.Tower;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings("unchecked")
public class IdClassTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testIdClassInSuperclass() throws Exception {
		Tower tower = new Tower();
		tower.latitude = 10.3;
		tower.longitude = 45.4;
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( tower );
		s.flush();
		s.clear();
		Location loc = new Location();
		loc.latitude = tower.latitude;
		loc.longitude = tower.longitude;
		assertNotNull( s.get( Tower.class, loc ) );
		tx.rollback();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Tower.class };
	}
}
