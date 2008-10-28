//$Id:HbmTest.java 9793 2006-04-26 02:20:18 -0400 (mer., 26 avr. 2006) epbernard $
package org.hibernate.test.annotations.xml.hbm;

import java.util.HashSet;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class HbmTest extends TestCase {

	public void testManyToOne() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		Government gov = new Government();
		gov.setName( "Liberals" );
		s.save( gov );
		PrimeMinister pm = new PrimeMinister();
		pm.setName( "Murray" );
		pm.setCurrentGovernment( gov );
		s.save( pm );
		s.getTransaction().rollback();
		s.close();
	}

	public void testOneToMany() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		Government gov = new Government();
		gov.setName( "Liberals" );
		Government gov2 = new Government();
		gov2.setName( "Liberals2" );
		s.save( gov );
		s.save( gov2 );
		PrimeMinister pm = new PrimeMinister();
		pm.setName( "Murray" );
		pm.setCurrentGovernment( gov );
		pm.setGovernments( new HashSet() );
		pm.getGovernments().add( gov2 );
		pm.getGovernments().add( gov );
		gov.setPrimeMinister( pm );
		gov2.setPrimeMinister( pm );
		s.save( pm );
		s.flush();
		s.getTransaction().rollback();
		s.close();
	}

	public void testManyToMany() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		CloudType type = new CloudType();
		type.setName( "Cumulus" );
		Sky sky = new Sky();
		s.persist( type );
		sky.getCloudTypes().add(type);
		s.persist( sky );
		s.flush();
		s.getTransaction().rollback();
		s.close();
	}

	public void testManyToOneAndInterface() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		B b = new BImpl();
		b.setBId( 1 );
		s.persist( b );
		Z z = new ZImpl();
		z.setB( b );
		s.persist( z );
		s.flush();
		s.getTransaction().rollback();
		s.close();
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		//cfg.addClass( Government.class );
	}

	protected Class[] getMappings() {
		return new Class[]{
				PrimeMinister.class,
				Sky.class,
				ZImpl.class

		};
	}

	@Override
	protected String[] getXmlFiles() {
		return new String[]{
				"org/hibernate/test/annotations/xml/hbm/Government.hbm.xml",
				"org/hibernate/test/annotations/xml/hbm/CloudType.hbm.xml",
				"org/hibernate/test/annotations/xml/hbm/A.hbm.xml",
				"org/hibernate/test/annotations/xml/hbm/B.hbm.xml"
		};
	}
}
