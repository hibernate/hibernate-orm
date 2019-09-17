/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Hibernate;
import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Gail Badner
 */
@SuppressWarnings({"unused", "WeakerAccess","ResultOfMethodCallIgnored"})
@TestForIssue( jiraKey = "HHH-13607" )
@RunWith( BytecodeEnhancerRunner.class )
@EnhancementOptions( lazyLoading = true )
public class NaturalIdInUninitializedProxyTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testImmutableNaturalId() {
		inTransaction(
				session -> {
					final EntityImmutableNaturalId e = session.getReference( EntityImmutableNaturalId.class, 1 );
					assertFalse( Hibernate.isInitialized( e ) );
				}
		);

		inTransaction(
				session -> {
					final EntityImmutableNaturalId e = session.get( EntityImmutableNaturalId.class, 1 );
					assertEquals( "name", e.name );
				}
		);
	}

	@Test
	public void testMutableNaturalId() {
		inTransaction(
				session -> {
					final EntityMutableNaturalId e = session.getReference( EntityMutableNaturalId.class, 1 );
					assertFalse( Hibernate.isInitialized( e ) );
				}
		);

		inTransaction(
				session -> {
					final EntityMutableNaturalId e = session.get( EntityMutableNaturalId.class, 1 );
					assertEquals( "name", e.name );
				}
		);
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true" );
		ssrb.applySetting( AvailableSettings.FORMAT_SQL, "false" );
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		super.configureSessionFactoryBuilder( sfb );
		sfb.applyStatisticsSupport( true );
		sfb.applySecondLevelCacheSupport( false );
		sfb.applyQueryCacheSupport( false );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( EntityMutableNaturalId.class );
		sources.addAnnotatedClass( EntityImmutableNaturalId.class );
	}

	@Before
	public void prepareTestData() {
		inTransaction(
				session -> {
					session.persist( new EntityMutableNaturalId( 1, "name" ) );
					session.persist( new EntityImmutableNaturalId( 1, "name" ) );
				}
		);
	}

	@After
	public void cleanUpTestData() {
		inTransaction(
				session -> {
					session.createQuery( "delete from EntityMutableNaturalId" ).executeUpdate();
					session.createQuery( "delete from EntityImmutableNaturalId" ).executeUpdate();
				}
		);
	}

	@Entity(name = "EntityMutableNaturalId")
	public static class EntityMutableNaturalId {
		@Id
		private int id;

		@NaturalId(mutable = true)
		private String name;

		public EntityMutableNaturalId() {
		}

		public EntityMutableNaturalId(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "EntityImmutableNaturalId")
	public static class EntityImmutableNaturalId {
		@Id
		private int id;

		@NaturalId(mutable = false)
		private String name;

		public EntityImmutableNaturalId() {
		}

		public EntityImmutableNaturalId(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}

}
