/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.xml.hbm;

import java.util.HashSet;

import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class HbmTest extends BaseCoreFunctionalTestCase {
	@Test
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

	@Test
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

	@Test
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

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				PrimeMinister.class,
				Sky.class,
		};
	}

	@Override
	protected String[] getXmlFiles() {
		return new String[]{
				"org/hibernate/test/annotations/xml/hbm/Government.hbm.xml",
				"org/hibernate/test/annotations/xml/hbm/CloudType.hbm.xml",
		};
	}
}
