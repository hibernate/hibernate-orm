/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.lob;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests of {@link org.hibernate.type.SerializableType}
 * 
 * @author Steve Ebersole
 */
public class SerializableTypeTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "lob/SerializableMappings.hbm.xml" };
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

	@Test
    @SkipForDialect( value = SybaseASE15Dialect.class, jiraKey = "HHH-6425")
	public void testNewSerializableType() {
		final String initialPayloadText = "Initial payload";
		final String changedPayloadText = "Changed payload";
		final String empty = "";

		Session s = openSession();
		s.beginTransaction();
		SerializableHolder holder = new SerializableHolder();
		s.save( holder );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		holder = ( SerializableHolder ) s.get( SerializableHolder.class, holder.getId() );
		assertNull( holder.getSerialData() );
		holder.setSerialData( new SerializableData( initialPayloadText ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		holder = ( SerializableHolder ) s.get( SerializableHolder.class, holder.getId() );
		SerializableData serialData = ( SerializableData ) holder.getSerialData();
		assertEquals( initialPayloadText, serialData.getPayload() );
		holder.setSerialData( new SerializableData( changedPayloadText ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		holder = ( SerializableHolder ) s.get( SerializableHolder.class, holder.getId() );
		serialData = ( SerializableData ) holder.getSerialData();
		assertEquals( changedPayloadText, serialData.getPayload() );
		holder.setSerialData( null );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		holder = ( SerializableHolder ) s.get( SerializableHolder.class, holder.getId() );
		assertNull( holder.getSerialData() );
		holder.setSerialData( new SerializableData( empty ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		holder = ( SerializableHolder ) s.get( SerializableHolder.class, holder.getId() );
		serialData = ( SerializableData ) holder.getSerialData();
		assertEquals( empty, serialData.getPayload() );
		s.delete( holder );
		s.getTransaction().commit();
		s.close();
	}

}
