/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
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
public class NaturalIdInUninitializedAssociationTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testImmutableNaturalId() {
		inTransaction(
				session -> {
					final AnEntity e = session.get( AnEntity.class, 3 );
					assertFalse( Hibernate.isPropertyInitialized( e,"entityImmutableNaturalId" ) );
				}
		);

		inTransaction(
				session -> {
					final AnEntity e = session.get( AnEntity.class, 3 );
					assertEquals( "immutable name", e.entityImmutableNaturalId.name );
				}
		);
	}

	@Test
	public void testMutableNaturalId() {
		inTransaction(
				session -> {
					final AnEntity e = session.get( AnEntity.class, 3 );
					assertFalse( Hibernate.isPropertyInitialized( e,"entityMutableNaturalId" ) );
				}
		);

		inTransaction(
				session -> {
					final AnEntity e = session.get( AnEntity.class, 3 );
					assertEquals( "mutable name", e.entityMutableNaturalId.name );
				}
		);
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
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
		sources.addAnnotatedClass( AnEntity.class );
		sources.addAnnotatedClass( EntityMutableNaturalId.class );
		sources.addAnnotatedClass( EntityImmutableNaturalId.class );
	}

	@Before
	public void prepareTestData() {
		inTransaction(
				session -> {
					EntityMutableNaturalId entityMutableNaturalId = new EntityMutableNaturalId( 1, "mutable name" );
					EntityImmutableNaturalId entityImmutableNaturalId = new EntityImmutableNaturalId( 2, "immutable name" );
					AnEntity anEntity = new AnEntity();
					anEntity.id = 3;
					anEntity.entityImmutableNaturalId = entityImmutableNaturalId;
					anEntity.entityMutableNaturalId = entityMutableNaturalId;
					session.persist( anEntity );
				}
		);
	}

	@After
	public void cleanUpTestData() {
		inTransaction(
				session -> {
					session.delete( session.get( AnEntity.class, 3 ) );
				}
		);
	}

	@Entity(name = "AnEntity")
	public static class AnEntity {
		@Id
		private int id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		@LazyToOne(LazyToOneOption.NO_PROXY )
		private EntityMutableNaturalId entityMutableNaturalId;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		@LazyToOne(LazyToOneOption.NO_PROXY )
		private EntityImmutableNaturalId entityImmutableNaturalId;
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
