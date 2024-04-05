/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

/**
 * Verifies that an object loaded via getReference and with
 * 1) batching enabled
 * 2) with bytecode enhancement
 * throws an ObjectNotFoundException as expected, when the object
 * doesn't exist in the database and a property is being read
 * forcing initialization.
 */
@DomainModel(
		annotatedClasses = {
				ReferenceLoadedEnhancedEntityTest.Country.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "10" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@JiraKey("HHH-16669")
public class ReferenceLoadedEnhancedEntityTest {

	@Test
	public void referenceLoadAlwaysWorks(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			//Materialize a reference to an object which doesn't exist
			final Country entity = s.getReference( Country.class, 1L );
			//should be fine..
		} );
	}

	@Test
	public void referenceNotExisting(SessionFactoryScope scope) {
		Exception exception = assertThrows( ObjectNotFoundException.class,
				() -> scope.inTransaction( s -> {
			//Materialize a reference to an object which doesn't exist
			final Country entity = s.getReference( Country.class, 1L );
			//This should fail:
			final String name = entity.getName();
			//Ensure we failed at the previous line:
			fail( "Should have thrown an ObjectNotFoundException exception before reaching this point" );
		} ) );
		assertNotNull( exception );
	}

	@Test
	public void referenceNotExisting2(SessionFactoryScope scope) {
		Exception exception = assertThrows( ObjectNotFoundException.class,
				() -> scope.inTransaction( s -> {
			//Materialize a reference to an object which doesn't exist
			final Country entity = s.getReference( Country.class, 1L );
			final Country entity2 = s.getReference( Country.class, 2L );
			//This should fail:
			final String name = entity.getName();
			//Ensure we failed at the previous line:
			fail( "Should have thrown an ObjectNotFoundException exception before reaching this point" );
		} ) );
		assertNotNull( exception );
	}

	@Test
	public void referenceNotExistingFieldAccess(SessionFactoryScope scope) {
		Exception exception = assertThrows( ObjectNotFoundException.class,
				() -> scope.inTransaction( s -> {
			//Materialize a reference to an object which doesn't exist
			final Country entity = s.getReference( Country.class, 1L );
			//This should fail:
			final String name = entity.name;
			//Ensure we failed at the previous line:
			fail( "Should have thrown an ObjectNotFoundException exception before reaching this point" );
		} ) );
		assertNotNull( exception );
	}

	@Test
	public void referenceNotExistingFieldAccess2(SessionFactoryScope scope) {
		Exception exception = assertThrows( ObjectNotFoundException.class,
				() -> scope.inTransaction( s -> {
			//Materialize a reference to an object which doesn't exist
			final Country entity = s.getReference( Country.class, 1L );
			final Country entity2 = s.getReference( Country.class, 2L );
			//This should fail:
			final String name = entity.name;
			//Ensure we failed at the previous line:
			fail( "Should have thrown an ObjectNotFoundException exception before reaching this point" );
		} ) );
		assertNotNull( exception );
	}

	@Entity
	@Table(name = "Country")
	public static class Country {
		@Id
		@GeneratedValue
		public Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
