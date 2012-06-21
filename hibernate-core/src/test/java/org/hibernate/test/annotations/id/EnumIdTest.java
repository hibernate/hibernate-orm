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
package org.hibernate.test.annotations.id;

import org.jboss.logging.Logger;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.id.entities.Planet;
import org.hibernate.test.annotations.id.entities.PlanetCheatSheet;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests for enum type as id.
 *
 * @author Hardy Ferentschik
 */
@SuppressWarnings("unchecked")
@TestForIssue( jiraKey = "ANN-744" )
public class EnumIdTest extends BaseCoreFunctionalTestCase {
	private static final Logger log = Logger.getLogger( EnumIdTest.class );

	@Test
	public void testEnumAsId() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		PlanetCheatSheet mercury = new PlanetCheatSheet();
		mercury.setPlanet(Planet.MERCURY);
		mercury.setMass(3.303e+23);
		mercury.setRadius(2.4397e6);
		mercury.setNumberOfInhabitants(0);
		s.persist(mercury);
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		PlanetCheatSheet mercuryFromDb = (PlanetCheatSheet) s.get(PlanetCheatSheet.class, mercury.getPlanet());
		assertNotNull(mercuryFromDb);
        log.debug(mercuryFromDb.toString());
		s.delete(mercuryFromDb);
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		mercury = (PlanetCheatSheet) s.get(PlanetCheatSheet.class, Planet.MERCURY);
		assertNull(mercury);
		tx.commit();
		s.close();
	}

	@Override
    protected Class[] getAnnotatedClasses() {
		return new Class[] { PlanetCheatSheet.class };
	}
}
