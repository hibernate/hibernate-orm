/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Hibernate;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@TestForIssue(jiraKey = "HHH-14571")
@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(lazyLoading = true, extendedEnhancement = true)
public class IdInUninitializedProxyTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { AnEntity.class };
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, true );
	}

	@Test
	public void testIdIsAlwaysConsideredInitialized() {
		inTransaction( session -> {
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

	@Before
	public void prepareTestData() {
		inTransaction( session -> {
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
