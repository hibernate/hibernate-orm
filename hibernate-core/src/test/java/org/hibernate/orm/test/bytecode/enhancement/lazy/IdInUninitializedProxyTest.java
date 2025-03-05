/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Hibernate;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKey("HHH-14571")
@DomainModel(
		annotatedClasses = {
				IdInUninitializedProxyTest.AnEntity.class
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true, extendedEnhancement = true)
public class IdInUninitializedProxyTest {

	@Test
	public void testIdIsAlwaysConsideredInitialized(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final AnEntity e = session.byId( AnEntity.class ).getReference( 1 );
			assertFalse( Hibernate.isInitialized( e ) );
			// This is the gist of the problem
			assertTrue( Hibernate.isPropertyInitialized( e, "id" ) );
			assertFalse( Hibernate.isPropertyInitialized( e, "name" ) );

			assertEquals( "George", e.name );
			assertTrue( Hibernate.isInitialized( e ) );
			assertTrue( Hibernate.isPropertyInitialized( e, "id" ) );
			assertTrue( Hibernate.isPropertyInitialized( e, "name" ) );
		} );
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			AnEntity anEntity = new AnEntity();
			anEntity.id = 1;
			anEntity.name = "George";
			session.persist( anEntity );
		} );
	}

	@Entity(name = "AnEntity")
	public static class AnEntity {
		@Id
		private int id;

		@Basic
		private String name;
	}

}
