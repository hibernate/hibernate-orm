//$Id: SubselectTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.subselect;

import java.util.Iterator;
import java.util.List;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class SubselectTest extends FunctionalTestCase {
	
	public SubselectTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "subselect/Beings.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( SubselectTest.class );
	}
	
	public void testEntitySubselect() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Human gavin = new Human();
		gavin.setName( "gavin" );
		gavin.setSex( 'M' );
		gavin.setAddress( "Melbourne, Australia" );
		Alien x23y4 = new Alien();
		x23y4.setIdentity( "x23y4$$hu%3" );
		x23y4.setPlanet( "Mars" );
		x23y4.setSpecies( "martian" );
		s.save(gavin);
		s.save(x23y4);
		s.flush();
		List beings = s.createQuery("from Being").list();
		for ( Iterator iter = beings.iterator(); iter.hasNext(); ) {
			Being b = (Being) iter.next();
			assertNotNull( b.getLocation() );
			assertNotNull( b.getIdentity() );
			assertNotNull( b.getSpecies() );
		}
		s.clear();
		getSessions().evict(Being.class);
		Being gav = (Being) s.get(Being.class, gavin.getId());
		assertEquals( gav.getLocation(), gavin.getAddress() );
		assertEquals( gav.getSpecies(), "human" );
		assertEquals( gav.getIdentity(), gavin.getName() );
		s.clear();
		//test the <synchronized> tag:
		gavin = (Human) s.get(Human.class, gavin.getId());
		gavin.setAddress( "Atlanta, GA" );
		gav = (Being) s.createQuery("from Being b where b.location like '%GA%'").uniqueResult();
		assertEquals( gav.getLocation(), gavin.getAddress() );
		s.delete(gavin);
		s.delete(x23y4);
		assertTrue( s.createQuery("from Being").list().isEmpty() );
		t.commit();
		s.close();
	}
	
	public void testCustomColumnReadAndWrite() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		final double HUMAN_INCHES = 73;
		final double ALIEN_INCHES = 931;
		final double HUMAN_CENTIMETERS = HUMAN_INCHES * 2.54d;		
		final double ALIEN_CENTIMETERS = ALIEN_INCHES * 2.54d;		
		Human gavin = new Human();
		gavin.setName( "gavin" );
		gavin.setSex( 'M' );
		gavin.setAddress( "Melbourne, Australia" );
		gavin.setHeightInches( HUMAN_INCHES );
		Alien x23y4 = new Alien();
		x23y4.setIdentity( "x23y4$$hu%3" );
		x23y4.setPlanet( "Mars" );
		x23y4.setSpecies( "martian" );
		x23y4.setHeightInches( ALIEN_INCHES );
		s.save(gavin);
		s.save(x23y4);
		s.flush();
		
		// Test value conversion during insert
		Double humanHeightViaSql = (Double)s.createSQLQuery("select height_centimeters from humans").uniqueResult();
		assertEquals(HUMAN_CENTIMETERS, humanHeightViaSql, 0.01d);
		Double alienHeightViaSql = (Double)s.createSQLQuery("select height_centimeters from aliens").uniqueResult();
		assertEquals(ALIEN_CENTIMETERS, alienHeightViaSql, 0.01d);
		s.clear();
		
		// Test projection
		Double heightViaHql = (Double)s.createQuery("select heightInches from Being b where b.identity = 'gavin'").uniqueResult();
		assertEquals(HUMAN_INCHES, heightViaHql, 0.01d);
		
		// Test restriction and entity load via criteria
		Being b = (Being)s.createCriteria(Being.class)
			.add(Restrictions.between("heightInches", HUMAN_INCHES - 0.01d, HUMAN_INCHES + 0.01d))
			.uniqueResult();
		assertEquals(HUMAN_INCHES, b.getHeightInches(), 0.01d);
		
		// Test predicate and entity load via HQL
		b = (Being)s.createQuery("from Being b where b.heightInches between ? and ?")
			.setDouble(0, ALIEN_INCHES - 0.01d)
			.setDouble(1, ALIEN_INCHES + 0.01d)
			.uniqueResult();
		assertEquals(ALIEN_INCHES, b.getHeightInches(), 0.01d);
                s.delete(gavin);
                s.delete(x23y4);		
		t.commit();
		s.close();
		
	}

}

