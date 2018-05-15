/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.crud;

import java.util.Collection;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.Component;
import org.hibernate.orm.test.support.domains.gambit.EntityOfSets;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class EntityOfSetsCrudTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( EntityOfSets.class );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@BeforeEach
	public void cleanUpTestData() {
		sessionFactoryScope().inTransaction(
				session -> {
					// select-and-delete to cascade deletes
					final List<EntityOfSets> results = session.createQuery( "select e from EntityOfSets e", EntityOfSets.class ).list();
					for ( EntityOfSets result : results ) {
						session.delete( result );
					}
				}
		);
	}

	@Test
	public void testOperations() {
		final EntityOfSets entity = new EntityOfSets( 1 );

		entity.getSetOfBasics().add( "first string" );
		entity.getSetOfBasics().add( "second string" );

		entity.getSetOfComponents().add(
				new Component(
						5,
						10L,
						15,
						"component string",
						new Component.Nested(
								"first nested string",
								"second nested string"
						)
				)
		);

		sessionFactoryScope().inTransaction( session -> session.save( entity ) );

		sessionFactoryScope().inSession(
				session -> {
					final Integer value = session.createQuery( "select e.id from EntityOfSets e", Integer.class ).uniqueResult();
					assert value == 1;
				}
		);

//		sessionFactoryScope().inSession(
//				session -> {
//					final EntityOfSets loaded = session.get( EntityOfSets.class, 1 );
//					assert loaded != null;
//					checkExpectedSize( loaded.getSetOfBasics(), 2 );
//				}
//		);

//		sessionFactoryScope().inSession(
//				session -> {
//					final List<EntityOfSets> list = session.byMultipleIds( EntityOfSets.class )
//							.multiLoad( 1, 2 );
//					assert list.size() == 1;
//					final EntityOfSets loaded = list.get( 0 );
//					assert loaded != null;
//					checkExpectedSize( loaded.getSetOfBasics(), 2 );
//				}
//		);
	}

	private void checkExpectedSize(Collection collection, int expectedSize) {
		if ( ! Hibernate.isInitialized( collection ) ) {
			Hibernate.initialize( collection );
		}

		assert Hibernate.isInitialized( collection );

		if ( collection.size() != expectedSize ) {
			Assert.fail(
					"Expecting Collection of size `" + expectedSize +
							"`, but passed Collection has `" + collection.size() + "` entries"
			);
		}

	}


	@Test
	public void testEagerOperations() {
		final EntityOfSets entity = new EntityOfSets( 1 );

		entity.getSetOfBasics().add( "first string" );
		entity.getSetOfBasics().add( "second string" );

		entity.getSetOfComponents().add(
				new Component(
						5,
						10L,
						15,
						"component string",
						new Component.Nested(
								"first nested string",
								"second nested string"
						)
				)
		);

		sessionFactoryScope().inTransaction( session -> session.save( entity ) );

		sessionFactoryScope().inTransaction(
				session -> {
					final Integer value = session.createQuery( "select e.id from EntityOfSets e", Integer.class ).uniqueResult();
					assert value == 1;
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					final EntityOfSets loaded = session.createQuery( "select e from EntityOfSets e left join fetch e.setOfBasics", EntityOfSets.class ).uniqueResult();
					assert loaded != null;
					checkExpectedSize( loaded.getSetOfBasics(), 2 );
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					final EntityOfSets loaded = session.createQuery( "select e from EntityOfSets e inner join fetch e.setOfBasics", EntityOfSets.class ).uniqueResult();
					assert loaded != null;
					checkExpectedSize( loaded.getSetOfBasics(), 2 );
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					final EntityOfSets loaded = session.createQuery( "select e from EntityOfSets e left join fetch e.setOfComponents", EntityOfSets.class ).uniqueResult();
					assert loaded != null;
					checkExpectedSize( loaded.getSetOfComponents(), 1 );
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					final EntityOfSets loaded = session.createQuery( "select e from EntityOfSets e inner join fetch e.setOfComponents", EntityOfSets.class ).uniqueResult();
					assert loaded != null;
					checkExpectedSize( loaded.getSetOfComponents(), 1 );
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					final EntityOfSets loaded = session.get( EntityOfSets.class, 1 );
					assert loaded != null;
					checkExpectedSize( loaded.getSetOfBasics(), 2 );
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					final List<EntityOfSets> list = session.byMultipleIds( EntityOfSets.class )
							.multiLoad( 1, 2 );
					assert list.size() == 1;
					final EntityOfSets loaded = list.get( 0 );
					assert loaded != null;
					checkExpectedSize( loaded.getSetOfBasics(), 2 );
				}
		);
	}
}
