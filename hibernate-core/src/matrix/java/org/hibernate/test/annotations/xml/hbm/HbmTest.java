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
package org.hibernate.test.annotations.xml.hbm;

import java.util.HashSet;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

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
