/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.naturalid.inheritance.cache;

import org.hibernate.WrongClassException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class InheritedNaturalIdCacheTest extends BaseCoreFunctionalTestCase {

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {MyEntity.class, ExtendedEntity.class};
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	public void testLoadingInheritedEntitiesByNaturalId() {
		// create the data:
		//		MyEntity#1
		//		ExtendedEntity#1

		inTransaction(
				session -> {
					session.save( new MyEntity( "base" ) );
					session.save( new ExtendedEntity( "extended", "ext" ) );
				}
		);

		// load the entities "properly" by natural-id

		inTransaction(
				session -> {
					final MyEntity entity = session.bySimpleNaturalId( MyEntity.class ).load( "base" );
					assertNotNull( entity );

					final ExtendedEntity extendedEntity = session.bySimpleNaturalId( ExtendedEntity.class ).load( "extended" );
					assertNotNull( extendedEntity );
				}
		);

		// finally, attempt to load MyEntity#1 as an ExtendedEntity, which should
		// throw a WrongClassException

		inTransaction(
				session -> {
					try {
						session.bySimpleNaturalId( ExtendedEntity.class ).load( "base" );
						fail( "Expecting WrongClassException" );
					}
					catch (WrongClassException expected) {
						// expected outcome
					}
					catch (Exception other) {
						throw new AssertionError(
								"Unexpected exception type : " + other.getClass().getName(),
								other
						);
					}
				}
		);

		// this is functionally equivalent to loading the wrong class by id...

		inTransaction(
				session -> {
					try {
						session.byId( ExtendedEntity.class ).load( 1L );
						fail( "Expecting WrongClassException" );
					}
					catch (WrongClassException expected) {
						// expected outcome
					}
					catch (Exception other) {
						throw new AssertionError(
								"Unexpected exception type : " + other.getClass().getName(),
								other
						);
					}
				}
		);

	}

}
