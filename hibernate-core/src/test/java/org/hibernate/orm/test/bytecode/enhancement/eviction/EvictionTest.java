/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.eviction;

import org.hibernate.engine.spi.ManagedEntity;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = {
			EvictionTest.Parent.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class EvictionTest {

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		// Create a Parent
		scope.inTransaction( s -> {
			Parent p = new Parent();
			p.name = "PARENT";
			s.persist( p );
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> {

			// Delete the Parent
			Parent loadedParent = (Parent) s.createQuery( "SELECT p FROM Parent p WHERE name=:name" )
					.setParameter( "name", "PARENT" )
					.uniqueResult();
			assertTyping( ManagedEntity.class, loadedParent );
			ManagedEntity managedParent = (ManagedEntity) loadedParent;

			// before eviction
			assertNotNull( managedParent.$$_hibernate_getEntityInstance() );
			assertNotNull( managedParent.$$_hibernate_getEntityEntry() );
			assertNull( managedParent.$$_hibernate_getPreviousManagedEntity() );
			assertNull( managedParent.$$_hibernate_getNextManagedEntity() );

			assertTrue( s.contains( managedParent ) );
			s.evict( managedParent );

			// after eviction
			assertFalse( s.contains( managedParent ) );
			assertNotNull( managedParent.$$_hibernate_getEntityInstance() );
			assertNull( managedParent.$$_hibernate_getEntityEntry() );
			assertNull( managedParent.$$_hibernate_getPreviousManagedEntity() );
			assertNull( managedParent.$$_hibernate_getNextManagedEntity() );

			// evict again
			s.evict( managedParent );

			assertFalse( s.contains( managedParent ) );
			assertNotNull( managedParent.$$_hibernate_getEntityInstance() );
			assertNull( managedParent.$$_hibernate_getEntityEntry() );
			assertNull( managedParent.$$_hibernate_getPreviousManagedEntity() );
			assertNull( managedParent.$$_hibernate_getNextManagedEntity() );

			s.remove( managedParent );
		} );
	}

	// --- //

	@Entity( name = "Parent" )
	@Table( name = "PARENT" )
	static class Parent {

		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		Long id;

		String name;
	}
}
