/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that an object loaded via getReference and with
 * 1) batching enabled
 * 2) with bytecode enhancement
 * throws an ObjectNotFoundException as expected, when the object
 * doesn't exist in the database and a property is being read
 * forcing initialization.
 */
@RunWith(BytecodeEnhancerRunner.class)
@JiraKey("HHH-16669")
public class ReferenceLoadedEnhancedEntityTest extends BaseCoreFunctionalTestCase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Country.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, "10" );
	}

	@Test
	public void referenceLoadAlwaysWorks() {
		doInHibernate( this::sessionFactory, s -> {
			//Materialize a reference to an object which doesn't exist
			final Country entity = s.getReference( Country.class, 1L );
			//should be fine..
		} );
	}

	@Test
	public void referenceNotExisting() {
		Exception exception = assertThrows( ObjectNotFoundException.class,
				() -> doInHibernate( this::sessionFactory, s -> {
			//Materialize a reference to an object which doesn't exist
			final Country entity = s.getReference( Country.class, 1L );
			//This should fail:
			final String name = entity.getName();
			//Ensure we failed at the previous line:
			Assert.fail( "Should have thrown an ObjectNotFoundException exception before reaching this point" );
		} ) );
		assertNotNull( exception );
	}

	@Test
	public void referenceNotExistingFieldAccess() {
		Exception exception = assertThrows( ObjectNotFoundException.class,
				() -> doInHibernate( this::sessionFactory, s -> {
			//Materialize a reference to an object which doesn't exist
			final Country entity = s.getReference( Country.class, 1L );
			//This should fail:
			final String name = entity.name;
			//Ensure we failed at the previous line:
			Assert.fail( "Should have thrown an ObjectNotFoundException exception before reaching this point" );
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
