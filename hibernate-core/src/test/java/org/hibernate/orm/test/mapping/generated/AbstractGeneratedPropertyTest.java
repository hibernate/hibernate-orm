/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;


import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Implementation of AbstractGeneratedPropertyTest.
 *
 * @author Steve Ebersole
 */
@SessionFactory
public abstract class AbstractGeneratedPropertyTest {

	@Test
	@JiraKey( value = "HHH-2627" )
	public final void testGeneratedProperty(SessionFactoryScope scope) {
		// The following block is repeated 300 times to reproduce HHH-2627.
		// Without the fix, Oracle will run out of cursors using 10g with
		// a default installation (ORA-01000: maximum open cursors exceeded).
		// The number of loops may need to be adjusted depending on how
		// Oracle is configured.
		// Note: The block is not indented to avoid a lot of irrelevant differences.
		for ( int i=0; i<300; i++ ) {
			final GeneratedPropertyEntity entity = new GeneratedPropertyEntity();
			entity.setName( "entity-1" );
			scope.inTransaction( session -> {
				session.persist( entity );
				session.flush();
				assertNotNull( entity.getLastModified(), "no timestamp retrieved" );
			} );

			byte[] bytes = entity.getLastModified();

			scope.inTransaction( session -> {
				GeneratedPropertyEntity _entity = session.find( GeneratedPropertyEntity.class, entity.getId() );
				assertTrue( PrimitiveByteArrayJavaType.INSTANCE.areEqual( bytes, _entity.getLastModified() ) );
			} );

			assertTrue( PrimitiveByteArrayJavaType.INSTANCE.areEqual( bytes, entity.getLastModified() ) );

			scope.inTransaction( session -> session.remove( entity ) );
		}
	}
}
