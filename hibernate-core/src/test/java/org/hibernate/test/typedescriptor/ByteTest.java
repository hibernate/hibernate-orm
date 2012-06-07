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
package org.hibernate.test.typedescriptor;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class ByteTest extends BaseCoreFunctionalTestCase {
	public static final byte TEST_VALUE = 65;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				VariousTypesEntity.class
		};
	}

	@Test
	@TestForIssue( jiraKey = "HHH-6533" )
	public void testByteDataPersistenceAndRetrieval() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();
		VariousTypesEntity entity = new VariousTypesEntity();
		entity.setId( 1 );
		entity.setByteData( TEST_VALUE );
		session.persist( entity );
		transaction.commit();
		session.close();

		// Testing sample value.
		session = openSession();
		transaction = session.beginTransaction();
		entity = (VariousTypesEntity) session.createQuery(
				" from VariousTypesEntity " +
						" where byteData = org.hibernate.test.typedescriptor.ByteTest.TEST_VALUE "
		).uniqueResult();
		Assert.assertNotNull( entity );
		Assert.assertEquals( TEST_VALUE, entity.getByteData() );
		entity.setByteData( Byte.MIN_VALUE );
		session.update( entity );
		transaction.commit();
		session.close();

		// Testing minimal value.
		session = openSession();
		transaction = session.beginTransaction();
		entity = (VariousTypesEntity) session.createQuery(
				" from VariousTypesEntity " +
						" where byteData = java.lang.Byte.MIN_VALUE "
		).uniqueResult();
		Assert.assertNotNull( entity );
		Assert.assertEquals( Byte.MIN_VALUE, entity.getByteData() );
		entity.setByteData( Byte.MAX_VALUE );
		session.update( entity );
		transaction.commit();
		session.close();

		// Testing maximal value.
		session = openSession();
		transaction = session.beginTransaction();
		entity = (VariousTypesEntity) session.createQuery(
				" from VariousTypesEntity " +
						" where byteData = java.lang.Byte.MAX_VALUE "
		).uniqueResult();
		Assert.assertNotNull( entity );
		Assert.assertEquals( Byte.MAX_VALUE, entity.getByteData() );
		transaction.commit();
		session.close();
	}
}
