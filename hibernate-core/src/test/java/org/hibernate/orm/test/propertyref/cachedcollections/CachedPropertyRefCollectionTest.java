/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.propertyref.cachedcollections;

import org.hibernate.Hibernate;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Set of tests originally developed to verify and fix HHH-5853
 *
 * @author Steve Ebersole
 */
@TestForIssue(jiraKey = "HHH-5853")
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/propertyref/cachedcollections/Mappings.hbm.xml"
)
@SessionFactory
public class CachedPropertyRefCollectionTest {

	private ManagedObject mo;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					mo = new ManagedObject( "test", "test" );
					mo.getMembers().add( "members" );
					session.persist( mo );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.remove( mo )
		);
	}

	@Test
	public void testRetrievalOfCachedCollectionWithPropertyRefKey(SessionFactoryScope scope) {
		// First attempt to load it via PK lookup
		scope.inTransaction(
				session -> {
					ManagedObject obj = session.get( ManagedObject.class, 1L );
					assertNotNull( obj );
					assertTrue( Hibernate.isInitialized( obj ) );
					obj.getMembers().size();
					assertTrue( Hibernate.isInitialized( obj.getMembers() ) );
				}
		);

		// Now try to access it via natural key
		scope.inTransaction(
				session -> {
					ManagedObject obj = session.bySimpleNaturalId( ManagedObject.class ).load( "test" );
					assertNotNull( obj );
					assertTrue( Hibernate.isInitialized( obj ) );
					obj.getMembers().size();
					assertTrue( Hibernate.isInitialized( obj.getMembers() ) );
				}
		);
	}
}

