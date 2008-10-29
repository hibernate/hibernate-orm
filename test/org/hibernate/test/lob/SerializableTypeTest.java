//$Id$
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
 *
 */
package org.hibernate.test.lob;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * Tests of {@link org.hibernate.type.SerializableType}
 * 
 * @author Steve Ebersole
 */
public class SerializableTypeTest extends FunctionalTestCase {

	public SerializableTypeTest(String testName) {
		super( testName );
	}

	public String[] getMappings() {
		return new String[] { "lob/LobMappings.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( SerializableTypeTest.class );
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

	public void testNewSerializableType() {
		final String payloadText = "Initial payload";

		Session s = openSession();
		s.beginTransaction();
		LobHolder holder = new LobHolder();
		holder.setSerialData( new SerializableData( payloadText ) );
		s.save( holder );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		holder = ( LobHolder ) s.get( LobHolder.class, holder.getId() );
		SerializableData serialData = ( SerializableData ) holder.getSerialData();
		assertEquals( payloadText, serialData.getPayload() );
		s.delete( holder );
		s.getTransaction().commit();
		s.close();
	}


}
