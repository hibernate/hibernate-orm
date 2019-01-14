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

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.gambit.EntityWithManyToOneWithoutJoinTable;
import org.hibernate.testing.orm.domain.gambit.EntityWithOneToManyNotOwned;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Chris Cranford
 */
public class EntityWithOneToManyWithoutJoinTableTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				EntityWithManyToOneWithoutJoinTable.class,
				EntityWithOneToManyNotOwned.class
		};
	}

	@AfterEach
	public void tearDown() {
		sessionFactoryScope().inTransaction(
				session -> {
					final EntityWithOneToManyNotOwned loaded = session.get(
							EntityWithOneToManyNotOwned.class,
							1
					);
					if ( loaded != null ) {
						List<EntityWithManyToOneWithoutJoinTable> children = loaded.getChildren();
						children.forEach( child -> session.remove( child ) );
						session.remove( loaded );
					}
				}
		);
	}

	@Test
	public void testSave() {
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
					List<EntityWithManyToOneWithoutJoinTable> children = retrieved.getChildren();

					assertFalse(
							Hibernate.isInitialized( children ),
							"The association should ne not initialized"

					);
					assertThat( children.size(), is( 2 ) );

					Map<Integer, EntityWithManyToOneWithoutJoinTable> othersById = new HashMap<>();
					for ( EntityWithManyToOneWithoutJoinTable child : children ) {
						othersById.put( child.getId(), child );
					}

					assertThat( othersById.get( 2 ).getSomeInteger(), is( Integer.MAX_VALUE ) );
					assertThat( othersById.get( 3 ).getSomeInteger(), is( Integer.MIN_VALUE ) );
				} );
	}

	@Test
	public void testSaveWithoutChildren() {
		EntityWithOneToManyNotOwned owner = new EntityWithOneToManyNotOwned();
		owner.setId( 1 );

		sessionFactoryScope().inTransaction(
				session -> {
					session.save( owner );
				} );

		sessionFactoryScope().inTransaction(
				session -> {
					EntityWithOneToManyNotOwned retrieved = session.get( EntityWithOneToManyNotOwned.class, 1 );
					assertThat( retrieved, notNullValue() );
					List<EntityWithManyToOneWithoutJoinTable> children = retrieved.getChildren();

					assertFalse(
							Hibernate.isInitialized( children ),
							"The association should ne not initialized"

					);
					assertThat( children.size(), is( 0 ) );
				} );
	}

	@Test
	public void testAddElements() {
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

		sessionFactoryScope().inTransaction(
				session -> {
					EntityWithOneToManyNotOwned retrieved = session.get( EntityWithOneToManyNotOwned.class, 1 );
					retrieved.addChild( child2 );
					session.save( child2 );
				} );

		sessionFactoryScope().inTransaction(
				session -> {
					EntityWithOneToManyNotOwned retrieved = session.get( EntityWithOneToManyNotOwned.class, 1 );
					assertThat( retrieved, notNullValue() );
					List<EntityWithManyToOneWithoutJoinTable> children = retrieved.getChildren();

					assertFalse(
							Hibernate.isInitialized( children ),
							"The association should ne not initialized"

					);
					assertThat( children.size(), is( 2 ) );

					Map<Integer, EntityWithManyToOneWithoutJoinTable> othersById = new HashMap<>();
					for ( EntityWithManyToOneWithoutJoinTable child : children ) {
						othersById.put( child.getId(), child );
					}

					assertThat( othersById.get( 2 ).getSomeInteger(), is( Integer.MAX_VALUE ) );
					assertThat( othersById.get( 3 ).getSomeInteger(), is( Integer.MIN_VALUE ) );
				} );
	}

	@Test
	public void testRemoveAllElements() {
		EntityWithOneToManyNotOwned owner = new EntityWithOneToManyNotOwned();
		owner.setId( 1 );

		EntityWithManyToOneWithoutJoinTable child1 = new EntityWithManyToOneWithoutJoinTable( 2, Integer.MAX_VALUE );

		owner.addChild( child1 );

		sessionFactoryScope().inTransaction(
				session -> {
					session.save( child1 );
					session.save( owner );
				} );


		sessionFactoryScope().inTransaction(
				session -> {
					EntityWithOneToManyNotOwned retrieved = session.get( EntityWithOneToManyNotOwned.class, 1 );

					List<EntityWithManyToOneWithoutJoinTable> children = retrieved.getChildren();
					session.delete( children.get( 0 ) );
					children.clear();
				} );

		sessionFactoryScope().inTransaction(
				session -> {
					EntityWithOneToManyNotOwned retrieved = session.get( EntityWithOneToManyNotOwned.class, 1 );
					assertThat( retrieved, notNullValue() );
					List<EntityWithManyToOneWithoutJoinTable> children = retrieved.getChildren();

					assertThat( children.size(), is( 0 ) );
				} );
	}

	@Test
	public void testRemoveOneElement() {
		EntityWithOneToManyNotOwned owner = new EntityWithOneToManyNotOwned();
		owner.setId( 1 );

		EntityWithManyToOneWithoutJoinTable child1 = new EntityWithManyToOneWithoutJoinTable( 2, Integer.MAX_VALUE );
		EntityWithManyToOneWithoutJoinTable child2 = new EntityWithManyToOneWithoutJoinTable( 3, Integer.MAX_VALUE );

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

					List<EntityWithManyToOneWithoutJoinTable> children = retrieved.getChildren();
					EntityWithManyToOneWithoutJoinTable toDelete = children.get( 0 );
					session.delete( toDelete );

					children.remove( toDelete );
				} );

		sessionFactoryScope().inTransaction(
				session -> {
					EntityWithOneToManyNotOwned retrieved = session.get( EntityWithOneToManyNotOwned.class, 1 );
					assertThat( retrieved, notNullValue() );
					List<EntityWithManyToOneWithoutJoinTable> children = retrieved.getChildren();

					assertThat( children.size(), is( 1 ) );
				} );
	}

}
