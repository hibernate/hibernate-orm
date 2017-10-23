/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.id;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.test.annotations.id.entities.Planet;
import org.hibernate.test.annotations.id.entities.PlanetCheatSheet;
import org.junit.Test;

import org.jboss.logging.Logger;

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
