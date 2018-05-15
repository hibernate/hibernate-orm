/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.lob;

import java.util.Arrays;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.type.MaterializedBlobType;
import org.hibernate.type.Type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@RequiresDialectFeature(DialectChecks.SupportsExpectedLobUsagePattern.class)
public class MaterializedBlobTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { MaterializedBlobEntity.class };
	}

	@Test
	public void testTypeSelection() {
		int index = sessionFactory().getEntityPersister( MaterializedBlobEntity.class.getName() ).getEntityMetamodel().getPropertyIndex( "theBytes" );
		Type  type = sessionFactory().getEntityPersister( MaterializedBlobEntity.class.getName() ).getEntityMetamodel().getProperties()[index].getType();
		assertEquals( MaterializedBlobType.INSTANCE, type );
	}

	@Test
	public void testSaving() {
		byte[] testData = "test data".getBytes();

		Session session = openSession();
		session.beginTransaction();
		MaterializedBlobEntity entity = new MaterializedBlobEntity( "test", testData );
		session.save( entity );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		entity = ( MaterializedBlobEntity ) session.get( MaterializedBlobEntity.class, entity.getId() );
		assertTrue( Arrays.equals( testData, entity.getTheBytes() ) );
		session.delete( entity );
		session.getTransaction().commit();
		session.close();
	}
}
