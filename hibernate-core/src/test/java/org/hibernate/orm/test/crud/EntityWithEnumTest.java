/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.crud;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.gambit.Shirt;

import org.junit.jupiter.api.Test;

/**
 * @author Chris Cranford
 */
public class EntityWithEnumTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Shirt.class,
		};
	}

	@Test
	public void testSavingEntityWithAnEnum() {
		final Shirt shirt = new Shirt();
		shirt.setId( 1 );
		shirt.setData( "X" );
		shirt.setColor( Shirt.Color.TAN );
		shirt.setSize( Shirt.Size.MEDIUM );

		inTransaction( session -> {session.createQuery( "delete Shirt" ).executeUpdate();} );
		inTransaction( session -> {session.save( shirt );} );
	}
}
