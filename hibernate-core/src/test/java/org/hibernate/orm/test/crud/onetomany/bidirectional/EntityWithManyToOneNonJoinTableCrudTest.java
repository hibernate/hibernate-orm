/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.crud.onetomany.bidirectional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.EntityWithManyToOneWithoutJoinTable;
import org.hibernate.orm.test.support.domains.gambit.EntityWithOneToManyNotOwned;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Chris Cranford
 */
public class EntityWithManyToOneNonJoinTableCrudTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( EntityWithManyToOneWithoutJoinTable.class );
		metadataSources.addAnnotatedClass( EntityWithOneToManyNotOwned.class );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Test
	public void testSaveOperation() {
		EntityWithOneToManyNotOwned owner = new EntityWithOneToManyNotOwned();
		owner.setId( 1 );

		EntityWithManyToOneWithoutJoinTable child1 = new EntityWithManyToOneWithoutJoinTable( 2, Integer.MAX_VALUE );
		EntityWithManyToOneWithoutJoinTable child2 = new EntityWithManyToOneWithoutJoinTable( 3, Integer.MIN_VALUE );
		owner.addChild( child1 );
		owner.addChild( child2 );

		sessionFactoryScope().inTransaction(
				session -> {
					session.save( child1 );
					session.save( child2 );
					session.save( owner );
				} );

		sessionFactoryScope().inTransaction(
				session -> {
					EntityWithOneToManyNotOwned retrieved = session.get( EntityWithOneToManyNotOwned.class, 1 );
					assertThat( retrieved, notNullValue() );
					List<EntityWithManyToOneWithoutJoinTable> childs = retrieved.getChilds();

					assertFalse(
							Hibernate.isInitialized( childs ),
							"The association should ne not initialized"

					);
					assertThat( childs.size(), is( 2 ) );

					Map<Integer, EntityWithManyToOneWithoutJoinTable> othersById = new HashMap<>();
					for ( EntityWithManyToOneWithoutJoinTable child : childs ) {
						othersById.put( child.getId(), child );
					}

					assertThat( othersById.get( 2 ).getSomeInteger(), is( Integer.MAX_VALUE ) );
					assertThat( othersById.get( 3 ).getSomeInteger(), is( Integer.MIN_VALUE ) );
				} );
	}

	@Test
	@Disabled("Update OneToMany association not yet implemented")
	public void testUpdateperation() {
		EntityWithOneToManyNotOwned owner = new EntityWithOneToManyNotOwned();
		owner.setId( 1 );

		EntityWithManyToOneWithoutJoinTable child1 = new EntityWithManyToOneWithoutJoinTable( 2, Integer.MAX_VALUE );

		owner.addChild( child1 );

		sessionFactoryScope().inTransaction(
				session -> {
					session.save( child1 );
					session.save( owner );
				} );

		EntityWithManyToOneWithoutJoinTable child2 = new EntityWithManyToOneWithoutJoinTable( 3, Integer.MIN_VALUE );
		owner.addChild( child2 );

		sessionFactoryScope().inTransaction(
				session -> {
					EntityWithOneToManyNotOwned retrieved = session.get( EntityWithOneToManyNotOwned.class, 1 );
					session.save( child2 );
				} );

		sessionFactoryScope().inTransaction(
				session -> {
					EntityWithOneToManyNotOwned retrieved = session.get( EntityWithOneToManyNotOwned.class, 1 );
					assertThat( retrieved, notNullValue() );
					List<EntityWithManyToOneWithoutJoinTable> childs = retrieved.getChilds();

					assertFalse(
							Hibernate.isInitialized( childs ),
							"The association should ne not initialized"

					);
					assertThat( childs.size(), is( 2 ) );

					Map<Integer, EntityWithManyToOneWithoutJoinTable> othersById = new HashMap<>();
					for ( EntityWithManyToOneWithoutJoinTable child : childs ) {
						othersById.put( child.getId(), child );
					}

					assertThat( othersById.get( 2 ).getSomeInteger(), is( Integer.MAX_VALUE ) );
					assertThat( othersById.get( 3 ).getSomeInteger(), is( Integer.MIN_VALUE ) );
				} );
	}
}
