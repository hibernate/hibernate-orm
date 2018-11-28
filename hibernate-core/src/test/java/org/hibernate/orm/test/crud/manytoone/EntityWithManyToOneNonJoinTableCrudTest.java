/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.crud.manytoone;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.EntityWithManyToOneNonJoinTable;
import org.hibernate.orm.test.support.domains.gambit.EntityWithOneToManyNotOwned;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Chris Cranford
 */
public class EntityWithManyToOneNonJoinTableCrudTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( EntityWithManyToOneNonJoinTable.class );
		metadataSources.addAnnotatedClass( EntityWithOneToManyNotOwned.class );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Test
	public void testOperations() {
		EntityWithOneToManyNotOwned owner = new EntityWithOneToManyNotOwned();
		owner.setId( 1 );

		EntityWithManyToOneNonJoinTable child = new EntityWithManyToOneNonJoinTable();
		child.setId( 2 );
		owner.addChild( child );

		sessionFactoryScope().inTransaction(
				session -> {
			session.save( child );
			session.save( owner );
			} );

		sessionFactoryScope().inTransaction(
				session -> {
					EntityWithOneToManyNotOwned retrieved = session.get( EntityWithOneToManyNotOwned.class, 1 );
					assertThat( retrieved, notNullValue() );
				} );
	}
}
