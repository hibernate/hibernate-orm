/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.ScrollableResults;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-11147" )
@RunWith( BytecodeEnhancerRunner.class )
@EnhancementOptions( lazyLoading = true )
public class JoinFetchScrollCollectionTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	@TestForIssue( jiraKey = "HHH-11147" )
	public void testScrollInStatelessSession() {
		inStatelessTransaction(
				session -> {
					final String qry = "select a from AnEntity a join fetch a.otherEntities order by a.id";
					final ScrollableResults results = session.createQuery( qry ).scroll();
					int idCounter = 1;
					for ( int i = 1 ; i <=3 ; i++ ) {
						assertTrue( results.next() );
						final AnEntity anEntity = (AnEntity) results.get( 0 );
						assertEquals( idCounter++, anEntity.id );
						assertTrue( Hibernate.isPropertyInitialized( anEntity, "otherEntities" ) );
						assertTrue( Hibernate.isInitialized( anEntity.otherEntities ) );
						assertEquals( i, anEntity.otherEntities.size() );
						final Set<Integer> expectedIds = new HashSet<>();
						for ( int j = 1 ; j <= i ; j++ ) {
							expectedIds.add( idCounter++ );
						}
						for ( OtherEntity otherEntity : anEntity.otherEntities ) {
							assertTrue( expectedIds.contains( otherEntity.id ) );
							assertSame( anEntity, otherEntity.anEntity );
						}
					}
					assertFalse( results.next() );
				}
		);
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PPROXY, "true" );
		ssrb.applySetting( AvailableSettings.FORMAT_SQL, "false" );
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		super.configureSessionFactoryBuilder( sfb );
		sfb.applySecondLevelCacheSupport( false );
		sfb.applyQueryCacheSupport( false );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( AnEntity.class );
		sources.addAnnotatedClass( OtherEntity.class );
	}

	@Before
	public void prepareTestData() {
		inTransaction(
				session -> {
					int idCounter = 1;
					for ( int i = 1; i <= 3; i++ ) {
						AnEntity anEntity = new AnEntity( idCounter++ );
						for ( int j = 1; j <= i; j++ ) {
							OtherEntity otherEntity = new OtherEntity( idCounter++ );
							anEntity.otherEntities.add( otherEntity );
							otherEntity.anEntity = anEntity;
						}
						session.persist( anEntity );
					}
				}
		);
	}

	@After
	public void cleanUpTestData() {
		inTransaction(
				session -> {
					for ( AnEntity anEntity : session.createQuery( "from AnEntity", AnEntity.class ).list() ) {
						session.delete( anEntity );
					}
				}
		);
	}

	@Entity(name="AnEntity")
	public static class AnEntity {

		@Id
		private int id;

		@OneToMany(mappedBy="anEntity", fetch= FetchType.LAZY, cascade = CascadeType.ALL)
		@LazyGroup("OtherEntities")
		private Set<OtherEntity> otherEntities = new HashSet<>();

		public AnEntity() {
		}

		public AnEntity(int id) {
			this.id = id;
		}

	}

	@Entity(name="OtherEntity")
	public static class OtherEntity {
		@Id
		private int id;

		@ManyToOne(fetch=FetchType.LAZY)
		@LazyGroup("AnEntity")
		@LazyToOne(LazyToOneOption.NO_PROXY )
		private AnEntity anEntity = null;

		public OtherEntity() {
		}

		public OtherEntity(int id) {
			this.id = id;
		}
	}
}
