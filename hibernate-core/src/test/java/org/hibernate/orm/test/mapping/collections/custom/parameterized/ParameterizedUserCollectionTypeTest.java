/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.parameterized;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tes for parameterized user collection types.
 *
 * @author Holger Brands
 * @author Steve Ebersole
 */
@SessionFactory
public abstract class ParameterizedUserCollectionTypeTest {
	@SuppressWarnings( {"unchecked"})
	@Test
	public void testBasicOperation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Entity entity = new Entity( "tester" );
			entity.getValues().add( "value-1" );
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			Entity entity = session.find( Entity.class, "tester" );
			assertTrue( Hibernate.isInitialized( entity.getValues() ) );
			assertEquals( 1, entity.getValues().size() );
			assertEquals( "Hello", ( ( DefaultableList ) entity.getValues() ).getDefaultValue() );
			session.remove( entity );
		} );
	}

}
