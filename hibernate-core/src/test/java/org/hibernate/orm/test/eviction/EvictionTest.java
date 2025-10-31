/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.eviction;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = IsolatedEvictableEntity.class)
@SessionFactory
public class EvictionTest {

	@Test
	@JiraKey(value = "HHH-7912")
	public void testNormalUsage(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new IsolatedEvictableEntity( 1 ) ) );

		var e = scope.fromTransaction( session -> {
			IsolatedEvictableEntity entity = session.get( IsolatedEvictableEntity.class, 1 );
			assertTrue( session.contains( entity ) );
			session.evict( entity );
			assertFalse( session.contains( entity ) );
			return entity;
		} );

		scope.inTransaction( session -> {
			session.remove( e );
		} );
	}

	@Test
	@JiraKey(value = "HHH-7912")
	public void testEvictingNull(SessionFactoryScope scope) {
		scope.inSession( session -> {
			try {
				session.evict( null );
				fail( "Expecting evict(null) to throw IAE" );
			}
			catch (IllegalArgumentException expected) {
			}
		} );
	}

	@Test
	@JiraKey(value = "HHH-7912")
	public void testEvictingTransientEntity(SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.evict( new IsolatedEvictableEntity( 1 ) );
		} );
	}

	@Test
	@JiraKey(value = "HHH-7912")
	public void testEvictingDetachedEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new IsolatedEvictableEntity( 1 ) );
		} );

		var e = scope.fromTransaction( session -> {
			IsolatedEvictableEntity entity = (IsolatedEvictableEntity) session.get( IsolatedEvictableEntity.class, 1 );
			assertTrue( session.contains( entity ) );
			// detach the entity
			session.evict( entity );
			assertFalse( session.contains( entity ) );
			// evict it again the entity
			session.evict( entity );
			assertFalse( session.contains( entity ) );
			return entity;
		} );

		scope.inTransaction( session -> {
			session.remove( e );
		});
	}

	@Test
	@JiraKey(value = "HHH-7912")
	public void testEvictingNonEntity(SessionFactoryScope scope) {
		scope.inSession( session -> {
			try {
				session.evict( new EvictionTest() );
				fail( "Expecting evict(non-entity) to throw IAE" );
			}
			catch (IllegalArgumentException expected) {
			}
		} );
	}

}
