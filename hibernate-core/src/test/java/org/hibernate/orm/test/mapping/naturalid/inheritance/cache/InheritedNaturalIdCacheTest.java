/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.naturalid.inheritance.cache;

import org.hibernate.WrongClassException;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@ServiceRegistry( settings = @Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ) )
@DomainModel( annotatedClasses = { MyEntity.class, ExtendedEntity.class } )
@SessionFactory
@NotImplementedYet( reason = "natural-id caching not yet implemented", strict = false )
public class InheritedNaturalIdCacheTest {
	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		// create the data:
		//		MyEntity#1
		//		ExtendedEntity#1
		scope.inTransaction(
				(session) -> {
					session.save( new MyEntity( "base" ) );
					session.save( new ExtendedEntity( "extended", "ext" ) );
				}
		);
	}

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.createQuery( "delete MyEntity" ).executeUpdate()
		);
	}

	@Test
	public void testLoadingInheritedEntitiesByNaturalId(SessionFactoryScope scope) {
		// load the entities "properly" by natural-id

		scope.inTransaction(
				(session) -> {
					final MyEntity entity = session.bySimpleNaturalId( MyEntity.class ).load( "base" );
					assertNotNull( entity );

					final ExtendedEntity extendedEntity = session.bySimpleNaturalId( ExtendedEntity.class ).load( "extended" );
					assertNotNull( extendedEntity );
				}
		);

		// finally, attempt to load MyEntity#1 as an ExtendedEntity, which should
		// throw a WrongClassException

		scope.inTransaction(
				(session) -> {
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

		scope.inTransaction(
				(session) -> {
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
