/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.crud;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.Shirt;

import org.junit.jupiter.api.Test;

/**
 * @author Chris Cranford
 */
public class EntityWithEnumTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( Shirt.class );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Test
	public void testSavingEntityWithAnEnum() {
		final Shirt shirt = new Shirt();
		shirt.setId( 1 );
		shirt.setData( "X" );
		shirt.setColor( Shirt.Color.TAN );
		shirt.setSize( Shirt.Size.MEDIUM );

		sessionFactoryScope().inTransaction( session -> session.createQuery( "delete Shirt" ).executeUpdate() );
		sessionFactoryScope().inTransaction( session -> session.save( shirt ) );
	}
}
