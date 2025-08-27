/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.otherentityentrycontext;


import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This task tests ManagedEntity objects that are already associated with a different PersistenceContext.
 *
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = {
			OtherEntityEntryContextTest.Parent.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class OtherEntityEntryContextTest {

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		// Create a Parent
		scope.inTransaction( s -> {
			s.persist( new Parent( 1L, "first" ) );
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Parent p = s.get( Parent.class, 1L );
			p.name = "third";

			s.merge( p );
			assertTrue( s.contains( p ) );
			s.evict( p );
			assertFalse( s.contains( p ) );

			p = s.get( Parent.class, p.id );

			assertEquals( "first", p.name );
		} );
	}

	// --- //

	@Entity
	@Table( name = "PARENT" )
	static class Parent {

		@Id
		Long id;

		String name;

		Parent() {
		}

		Parent(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
