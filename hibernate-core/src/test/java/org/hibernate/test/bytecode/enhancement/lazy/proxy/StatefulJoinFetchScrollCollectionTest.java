/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-13405" )
@RunWith( BytecodeEnhancerRunner.class )
@EnhancementOptions( lazyLoading = true )
public class StatefulJoinFetchScrollCollectionTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	@TestForIssue( jiraKey = "HHH-13405" )
	public void testScrollInStatefulSession() {
		inTransaction(
				session -> {
					int idCounter = 1;
					for ( int i = 1; i <= 3; i++ ) {
						AEntity aEntity = new AEntity( idCounter++ );
						for ( int j = 1; j <= i; j++ ) {
							BEntity bEntity = new BEntity( idCounter++ );
							aEntity.bEntities.add( bEntity );
							bEntity.aEntity = aEntity;
						}
						session.persist( aEntity );
					}
				}
		);

		inTransaction(
				session -> {
					final String qry = "select a from AEntity a join fetch a.bEntities order by a.id";
					final ScrollableResults results = session.createQuery( qry ).scroll();
					int idCounter = 1;
					for ( int i = 1; i <= 3; i++ ) {
						assertTrue( results.next() );
						final AEntity aEntity = (AEntity) results.get( 0 );
						assertEquals( idCounter++, aEntity.id );
						assertTrue( Hibernate.isPropertyInitialized( aEntity, "bEntities" ) );
						assertTrue( Hibernate.isInitialized( aEntity.bEntities ) );
						assertEquals( i, aEntity.bEntities.size() );
						final Set<Integer> expectedIds = new HashSet<>();
						for ( int j = 1; j <= i; j++ ) {
							expectedIds.add( idCounter++ );
						}
						for ( BEntity bEntity : aEntity.bEntities ) {
							assertTrue( expectedIds.contains( bEntity.id ) );
							assertSame( aEntity, bEntity.aEntity );
						}
					}
					assertFalse( results.next() );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13405" )
	public void testDistinctListInStatefulSession() {
		inTransaction(
				session -> {
					AEntity a = new AEntity( 1 );
					BEntity b1 = new BEntity( 2 );
					BEntity b2 = new BEntity( 3 );
					CEntity c = new CEntity( 4 );

					b1.aEntity = a;
					b2.aEntity = a;
					b1.cEntity = c;
					b2.cEntity = c;

					session.persist( a );
					session.persist( b1 );
					session.persist( b2 );
					session.persist( c );
				}
		);

		final String hql = "Select distinct a from AEntity a left join fetch a.bEntities order by a.id";

		inTransaction(
				session -> {
					List result = session.createQuery( hql ).list();
					assertEquals( 1, result.size() );
					for ( Object obj : result ) {
						AEntity a = (AEntity) obj;
						assertTrue( Hibernate.isPropertyInitialized( a, "bEntities" ) );
						assertTrue( Hibernate.isInitialized( a.bEntities ) );
						CEntity cEntity = null;
						for ( BEntity b : a.bEntities ) {
							assertSame( a, b.aEntity );
							assertTrue( Hibernate.isPropertyInitialized( b, "cEntity" ) );
							if ( cEntity != null ) {
								assertSame( cEntity, b.cEntity );
							}
							else {
								cEntity = b.cEntity;
							}
						}
						assertEnhancedProxy( cEntity );
					}
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13405" )
	public void testDistinctScrollInStatefulSession() {
		inTransaction(
				session -> {
					AEntity a = new AEntity( 1 );
					BEntity b1 = new BEntity( 2 );
					BEntity b2 = new BEntity( 3 );
					CEntity c = new CEntity( 4 );

					b1.aEntity = a;
					b2.aEntity = a;
					b1.cEntity = c;
					b2.cEntity = c;

					session.persist(a);
					session.persist(b1);
					session.persist( b2 );
					session.persist( c );
				}
		);

		final String hql = "Select distinct a from AEntity a left join fetch a.bEntities order by a.id";

		inTransaction(
				session -> {
					final ScrollableResults rs = session.createQuery( hql ).scroll( ScrollMode.SCROLL_INSENSITIVE );
					assertTrue( rs.next() );
					AEntity a = (AEntity) rs.get( 0 );
					assertTrue( Hibernate.isPropertyInitialized( a, "bEntities" ) );
					assertTrue( Hibernate.isInitialized( a.bEntities ) );
					CEntity cEntity = null;
					for ( BEntity b : a.bEntities ) {
						assertSame( a, b.aEntity );
						assertTrue( Hibernate.isPropertyInitialized( b, "cEntity" ) );
						if ( cEntity != null ) {
							assertSame( cEntity, b.cEntity );
						}
						else {
							cEntity = b.cEntity;
						}
					}
					assertEnhancedProxy( cEntity );
					assertFalse( rs.next() );
				}
		);
	}

	private final void assertEnhancedProxy(Object entity) {
		assertTrue( PersistentAttributeInterceptable.class.isInstance( entity ) );
		final BytecodeEnhancementMetadata enhancementMetadata =
				sessionFactory().getMetamodel().entityPersister( entity.getClass() ).getEntityMetamodel()
						.getBytecodeEnhancementMetadata();
		final BytecodeLazyAttributeInterceptor interceptor = enhancementMetadata.extractLazyInterceptor( entity );
		assertTrue( EnhancementAsProxyLazinessInterceptor.class.isInstance( interceptor ) );
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
		sources.addAnnotatedClass( AEntity.class );
		sources.addAnnotatedClass( BEntity.class );
		sources.addAnnotatedClass( CEntity.class );
	}

	@After
	public void cleanUpTestData() {
		inTransaction(
				session -> {
					for ( AEntity aEntity : session.createQuery( "from AEntity", AEntity.class ).list() ) {
						session.delete( aEntity );
					}
				}
		);
	}

	@Entity(name="AEntity")
	public static class AEntity {

		@Id
		private int id;

		@OneToMany(mappedBy="aEntity", fetch= FetchType.LAZY, cascade = CascadeType.ALL)
		private Set<BEntity> bEntities = new HashSet<>();

		public AEntity() {
		}

		public AEntity(int id) {
			this.id = id;
		}

	}

	@Entity(name="BEntity")
	public static class BEntity {
		@Id
		private int id;

		@ManyToOne(fetch=FetchType.LAZY)
		@LazyGroup("AEntity")
		@LazyToOne(LazyToOneOption.NO_PROXY )
		private AEntity aEntity = null;

		@ManyToOne(fetch=FetchType.LAZY, cascade = CascadeType.ALL)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		@LazyGroup("cEntity")
		protected CEntity cEntity = null;

		public BEntity() {
		}

		public BEntity(int id) {
			this.id = id;
		}
	}

	@Entity(name="CEntity")
	public static class CEntity {

		@Id
		private int id;

		public CEntity(int id) {
			this();
			this.id = id;
			bEntities = new LinkedHashSet();
		}

		protected CEntity() {
			// this form used by Hibernate
		}

		public Set<BEntity> getBEntities() {
			return bEntities;
		}

		@OneToMany(mappedBy="cEntity", fetch=FetchType.LAZY)
		protected Set<BEntity> bEntities = null;
	}
}
