/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Implementation of AbstractGeneratedPropertyTest.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractGeneratedPropertyTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Test
	@JiraKey( value = "HHH-2627" )
	public final void testGeneratedProperty() {
		// The following block is repeated 300 times to reproduce HHH-2627.
		// Without the fix, Oracle will run out of cursors using 10g with
		// a default installation (ORA-01000: maximum open cursors exceeded).
		// The number of loops may need to be adjusted depending on the how
		// Oracle is configured.
		// Note: The block is not indented to avoid a lot of irrelevant differences.
		for ( int i=0; i<300; i++ ) {
			GeneratedPropertyEntity entity = new GeneratedPropertyEntity();
			entity.setName( "entity-1" );
			Session s = openSession();
			Transaction t = s.beginTransaction();
			s.persist( entity );
			s.flush();
			assertNotNull( "no timestamp retrieved", entity.getLastModified() );
			t.commit();
			s.close();

			byte[] bytes = entity.getLastModified();

			s = openSession();
			t = s.beginTransaction();
			entity = ( GeneratedPropertyEntity ) s.get( GeneratedPropertyEntity.class, entity.getId() );
			assertTrue( PrimitiveByteArrayJavaType.INSTANCE.areEqual( bytes, entity.getLastModified() ) );
			t.commit();
			s.close();

			assertTrue( PrimitiveByteArrayJavaType.INSTANCE.areEqual( bytes, entity.getLastModified() ) );

			s = openSession();
			t = s.beginTransaction();
			s.remove( entity );
			t.commit();
			s.close();
		}
	}
}
