//$Id$
package org.hibernate.test.annotations.id.sequences;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.test.annotations.id.sequences.entities.Planet;
import org.hibernate.test.annotations.id.sequences.entities.PlanetCheatSheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for enum type as id.
 * 
 * @author Hardy Ferentschik
 * @see ANN-744
 */
@SuppressWarnings("unchecked")
public class EnumIdTest extends TestCase {

	private Logger log = LoggerFactory.getLogger(EnumIdTest.class);	
	
	public EnumIdTest(String x) {
		super(x);
	}

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

	/**
	 * @see org.hibernate.test.annotations.TestCase#getAnnotatedClasses()
	 */
	protected Class[] getAnnotatedClasses() {
		return new Class[] { PlanetCheatSheet.class };
	}
}
