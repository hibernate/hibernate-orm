/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.lob;

import java.util.Arrays;

import org.junit.Test;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;

import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.Type;

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
		assertEquals( StandardBasicTypes.MATERIALIZED_BLOB, type );
	}

	@Test
	public void testSaving() {
		byte[] testData = "test data".getBytes();
		final Long entityId = TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			MaterializedBlobEntity entity = new MaterializedBlobEntity( "test", testData );
			session.save( entity );
			return entity.getId();
		} );

		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			MaterializedBlobEntity entity = session.get( MaterializedBlobEntity.class, entityId );
			assertTrue( Arrays.equals( testData, entity.getTheBytes() ) );
			session.delete( entity );
		} );
	}
}
