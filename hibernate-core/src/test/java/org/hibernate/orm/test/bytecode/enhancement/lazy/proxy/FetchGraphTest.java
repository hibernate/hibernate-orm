/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import java.sql.Blob;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.ScrollableResults;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.Query;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@JiraKey("HHH-11147")
@DomainModel(
		annotatedClasses = {
				FetchGraphTest.AEntity.class,
				FetchGraphTest.BEntity.class,
				FetchGraphTest.CEntity.class,
				FetchGraphTest.DEntity.class,
				FetchGraphTest.EEntity.class,
				FetchGraphTest.GEntity.class,
				Activity.class,
				Instruction.class,
				WebApplication.class,
				SpecializedKey.class,
				MoreSpecializedKey.class,
				RoleEntity.class,
				AbstractKey.class,
				GenericKey.class,
				SpecializedEntity.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.FORMAT_SQL, value = "false" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "false" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true)
public class FetchGraphTest {

	@Test
	public void testLoadNonOwningOneToOne(SessionFactoryScope scope) {
		// Test loading D and accessing E
		// 		E is the owner of the FK, not D.  When `D#e` is accessed we
		//		need to actually load E because its table has the FK value, not
		//		D's table

		final StatisticsImplementor stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		assert scope.getSessionFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( DEntity.class )
				.getBytecodeEnhancementMetadata()
				.isEnhancedForLazyLoading();

		scope.inSession(
				session -> {
					final DEntity entityD = session.getReference( DEntity.class, 1L );
					assertThat( stats.getPrepareStatementCount(), is( 0L ) );
					assert !Hibernate.isPropertyInitialized( entityD, "a" );
					assert !Hibernate.isPropertyInitialized( entityD, "c" );
					assert !Hibernate.isPropertyInitialized( entityD, "e" );

					entityD.getE();

					assertThat( stats.getPrepareStatementCount(), is( 2L ) );
					assert Hibernate.isPropertyInitialized( entityD, "a" );
					assert !Hibernate.isInitialized( entityD.getA() );
					assert Hibernate.isPropertyInitialized( entityD, "c" );
					assert !Hibernate.isInitialized( entityD.getC() );
					assert Hibernate.isPropertyInitialized( entityD, "e" );
					assert Hibernate.isInitialized( entityD.getE() );
				}
		);
	}

	@Test
	public void testLoadOwningOneToOne(SessionFactoryScope scope) {
		// switch it around

		final StatisticsImplementor stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		assert scope.getSessionFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( DEntity.class )
				.getBytecodeEnhancementMetadata()
				.isEnhancedForLazyLoading();

		scope.inSession(
				session -> {
					final EEntity entityE = session.getReference( EEntity.class, 17L );
					assertThat( stats.getPrepareStatementCount(), is( 0L ) );
					assert !Hibernate.isPropertyInitialized( entityE, "d" );

					final DEntity entityD = entityE.getD();
					assertThat( stats.getPrepareStatementCount(), is( 1L ) );
					assert !Hibernate.isPropertyInitialized( entityD, "a" );
					assert !Hibernate.isPropertyInitialized( entityD, "c" );
					assert !Hibernate.isPropertyInitialized( entityD, "e" );
				}
		);
	}

	@Test
	public void basicTypeLazyGroup(SessionFactoryScope scope) {

		scope.inSession(
				session -> {
					final String qry = "select d from D d";

					final Query query = session.createQuery( qry );
					try (ScrollableResults scrollableResults = query.scroll()) {
						while ( scrollableResults.next() ) {
							final DEntity dEntity = (DEntity) scrollableResults.get();
							assertFalse( Hibernate.isPropertyInitialized( dEntity, "blob" ) );
							assertFalse( Hibernate.isPropertyInitialized( dEntity, "lazyString" ) );
							assertFalse( Hibernate.isPropertyInitialized( dEntity, "lazyStringBlobGroup" ) );
							assertTrue( Hibernate.isPropertyInitialized( dEntity, "nonLazyString" ) );
						}
					}
				}
		);

		scope.inSession(
				session -> {
					final DEntity dEntity = session.get( DEntity.class, 1L );
					assertFalse( Hibernate.isPropertyInitialized( dEntity, "blob" ) );
					assertFalse( Hibernate.isPropertyInitialized( dEntity, "lazyString" ) );
					assertFalse( Hibernate.isPropertyInitialized( dEntity, "lazyStringBlobGroup" ) );

					assertTrue( Hibernate.isPropertyInitialized( dEntity, "nonLazyString" ) );

					dEntity.getBlob();

					assertTrue( Hibernate.isPropertyInitialized( dEntity, "blob" ) );
					assertTrue( Hibernate.isPropertyInitialized( dEntity, "lazyStringBlobGroup" ) );

					assertFalse( Hibernate.isPropertyInitialized( dEntity, "lazyString" ) );
				}
		);

		scope.inSession(
				session -> {
					final DEntity dEntity = session.get( DEntity.class, 1L );
					assertFalse( Hibernate.isPropertyInitialized( dEntity, "blob" ) );
					assertFalse( Hibernate.isPropertyInitialized( dEntity, "lazyString" ) );
					assertFalse( Hibernate.isPropertyInitialized( dEntity, "lazyStringBlobGroup" ) );

					assertTrue( Hibernate.isPropertyInitialized( dEntity, "nonLazyString" ) );
					dEntity.getBlob();

					assertTrue( Hibernate.isPropertyInitialized( dEntity, "blob" ) );
					assertTrue( Hibernate.isPropertyInitialized( dEntity, "lazyStringBlobGroup" ) );

					assertFalse( Hibernate.isPropertyInitialized( dEntity, "lazyString" ) );
				}
		);

		scope.inSession(
				session -> {
					final String qry = "select e from E e join fetch e.d";

					final Query query = session.createQuery( qry );
					final List<EEntity> results = query.list();
					results.forEach(
							eEntity -> {
								final DEntity dEntity = eEntity.getD();
								assertFalse( Hibernate.isPropertyInitialized( dEntity, "blob" ) );
								assertFalse( Hibernate.isPropertyInitialized( dEntity, "lazyString" ) );
								assertFalse( Hibernate.isPropertyInitialized( dEntity, "lazyStringBlobGroup" ) );

								assertTrue( Hibernate.isPropertyInitialized( dEntity, "nonLazyString" ) );
								assertThat( dEntity.getNonLazyString(), is("I am not lazy") );

								dEntity.getBlob();

								assertTrue( Hibernate.isPropertyInitialized( dEntity, "blob" ) );

								assertTrue( Hibernate.isPropertyInitialized( dEntity, "lazyStringBlobGroup" ) );
								assertThat( dEntity.getLazyStringBlobGroup(), is("I am lazy as a blob") );

								assertFalse( Hibernate.isPropertyInitialized( dEntity, "lazyString" ) );
								assertThat( dEntity.getLazyString(), is("I am lazy") );
							}
					);
				}
		);
	}

	@Test
	public void testFetchingScroll(SessionFactoryScope scope) {
		final StatisticsImplementor stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		assert scope.getSessionFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( DEntity.class )
				.getBytecodeEnhancementMetadata()
				.isEnhancedForLazyLoading();

		scope.inStatelessSession(
				session -> {
					final String qry = "select e from E e join fetch e.d";

					final Query query = session.createQuery( qry );
					try (ScrollableResults scrollableResults = query.scroll()) {
						while ( scrollableResults.next() ) {
							final EEntity eEntity = (EEntity) scrollableResults.get();
							final DEntity dEntity = eEntity.getD();
							assertFalse( Hibernate.isPropertyInitialized( dEntity, "blob" ) );
							assertTrue( Hibernate.isPropertyInitialized( dEntity, "nonLazyString" ) );
						}
					}
				}
		);

		scope.inStatelessSession(
				session -> {
					final String qry = "select d from D d " +
							"join fetch d.a " +
							"join fetch d.bs " +
							"join fetch d.c " +
							"join fetch d.e " +
							"join fetch d.g";

					final Query query = session.createQuery( qry );
					try (ScrollableResults scrollableResults = query.scroll()) {
						int i = 0;
						while ( scrollableResults.next() ) {
							i++;
							final DEntity dEntity = (DEntity) scrollableResults.get();
							assertThat( dEntity.getBs().size(), is( 2 ) );
						}
						assertThat( i, is( 1 ) );
					}
				}
		);

		scope.inStatelessSession(
				session -> {
					final String qry = "select g from G g join fetch g.dEntities";

					final Query query = session.createQuery( qry );
					try (ScrollableResults scrollableResults = query.scroll()) {
						while ( scrollableResults.next() ) {
							final Object o = scrollableResults.get();
						}
					}
				}
		);
	}

	@Test
	public void testFetchingScroll2(SessionFactoryScope scope) {
		final StatisticsImplementor stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		assert scope.getSessionFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( DEntity.class )
				.getBytecodeEnhancementMetadata()
				.isEnhancedForLazyLoading();

		scope.inStatelessSession(
				session -> {
					final String qry = "select d, d.d from D d " +
							"join fetch d.a " +
							"join fetch d.bs " +
							"join fetch d.c " +
							"join fetch d.e " +
							"join fetch d.g";

					final Query query = session.createQuery( qry );
					try (ScrollableResults scrollableResults = query.scroll()) {
						int i = 0;
						while ( scrollableResults.next() ) {
							i++;
							final Object[] result = (Object[]) scrollableResults.get();
							final DEntity dEntity = (DEntity) result[0];
							assertThat( dEntity.getBs().size(), is( 2 ) );
							assertThat( result[1], is( "bla" ) );
						}
						assertThat( i, is( 1 ) );
					}
				}
		);
	}

	private ScrollableResults getScrollableResults(Query query) {
		return query.scroll();
	}

	@Test
	public void testLazyAssociationSameAsNonLazyInPC(SessionFactoryScope scope) {

		assert scope.getSessionFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( DEntity.class )
				.getBytecodeEnhancementMetadata()
				.isEnhancedForLazyLoading();

		scope.inSession(
				session -> {
					final AEntity entityA = session.get( AEntity.class, 1L );

					final DEntity entityD = session.getReference( DEntity.class, 1L );
					assert !Hibernate.isInitialized( entityD );
					Hibernate.initialize( entityD );
					assert Hibernate.isPropertyInitialized( entityD, "a" );
					assert entityA.getOid() == entityD.getA().getOid();
					assert session.getPersistenceContext().getEntry( entityA ) ==
							session.getPersistenceContext().getEntry( entityD.getA() );
					assert entityA == entityD.getA();
				}
		);
	}

	@Test
	public void testRandomAccess(SessionFactoryScope scope) {
		final StatisticsImplementor stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		final EntityPersister persister = scope.getSessionFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( DEntity.class );
		assert persister.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();

		scope.inSession(
				session -> {
					// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					// Load a D

					final DEntity entityD = session.getReference( DEntity.class, 1L );

					assertThat( entityD instanceof HibernateProxy, is( false ) );
					assertThat( entityD instanceof PersistentAttributeInterceptable, is( true ) );
					assertThat( Hibernate.isInitialized( entityD ), is( false ) );
					// Because D is enhanced we should not have executed any SQL
					assertThat( stats.getPrepareStatementCount(), is( 0L ) );

					// access the id.
					// 		-since entityD is a "enhanced proxy", this should not trigger loading
					assertThat( entityD.getOid(), is( 1L ) );
					assertThat( Hibernate.isInitialized( entityD ), is( false ) );
					assertThat( stats.getPrepareStatementCount(), is( 0L ) );


					// Because D is enhanced we should not have executed any SQL
					assertThat( stats.getPrepareStatementCount(), is( 0L ) );

					assert !Hibernate.isPropertyInitialized( entityD, "a" );
					assert !Hibernate.isPropertyInitialized( entityD, "c" );
					assert !Hibernate.isPropertyInitialized( entityD, "e" );
					assert !Hibernate.isPropertyInitialized( entityD, "g" );


					// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					// Access C

					final CEntity c = entityD.getC();

					// make sure interception happened
					assertThat( c, notNullValue() );

					// See `#testLoadNonOwningOneToOne`
					assertThat( stats.getPrepareStatementCount(), is( 1L ) );

					// The fields themselves are initialized - set to the
					// enhanced entity "proxy" instance
					assert Hibernate.isPropertyInitialized( entityD, "a" );
					assert Hibernate.isPropertyInitialized( entityD, "c" );
					assert !Hibernate.isPropertyInitialized( entityD, "e" );

					assert !Hibernate.isInitialized( entityD.getA() );
					assert !Hibernate.isInitialized( entityD.getC() );


					// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					// Access C again

					entityD.getC();
					assertThat( stats.getPrepareStatementCount(), is( 1L ) );


					// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					// Access E

					final EEntity e1 = entityD.getE();
					assert Hibernate.isPropertyInitialized( entityD, "e" );

					assert Hibernate.isInitialized( entityD.getE() );

					assertThat( stats.getPrepareStatementCount(), is( 2L ) );
					assert Hibernate.isInitialized( e1 );


					// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					// Access E again

					entityD.getE();
					assertThat( stats.getPrepareStatementCount(), is( 2L ) );

					assertThat( entityD.getE().getOid(), is( 17L ) );


					// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					// now lets access the attribute "proxies"

					// this will load the table C data
					entityD.getC().getC1();
					assertThat( stats.getPrepareStatementCount(), is( 3L ) );
					assert Hibernate.isInitialized( entityD.getC() );

					// this should not - it was already loaded above
					entityD.getE().getE1();
					assertThat( stats.getPrepareStatementCount(), is( 3L ) );

					Set<BEntity> bs = entityD.getBs();
					assertThat( stats.getPrepareStatementCount(), is( 3L ) );
					assertThat( bs.size(), is( 2 ) );
					assertThat( stats.getPrepareStatementCount(), is( 4L ) );

					entityD.getG().getOid();
					assertThat( stats.getPrepareStatementCount(), is( 4L ) );
				}
		);
	}

	@Test
	public void testNullManyToOneHql(SessionFactoryScope scope) {
		final StatisticsImplementor stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		scope.inTransaction(
				session -> {
					final String qry = "select e from Activity e";
					final List<Activity> activities = session.createQuery( qry, Activity.class ).list();

					assertThat( stats.getPrepareStatementCount(), is( 1L ) );

					long expectedCount = 1L;

					for ( Activity activity : activities ) {
						if ( activity.getInstruction() != null ) {
							// do something special
							//		- here we just access an attribute to trigger
							//		the initialization of the association

							activity.getInstruction().getSummary();
							assertThat( stats.getPrepareStatementCount(), is( ++expectedCount ) );
						}

						if ( activity.getWebApplication() != null ) {
							// trigger base group initialization
							activity.getWebApplication().getName();
							assertThat( stats.getPrepareStatementCount(), is( ++expectedCount ) );
							// trigger  initialization
							activity.getWebApplication().getSiteUrl();
							assertThat( stats.getPrepareStatementCount(), is( ++expectedCount ) );
						}
					}
				}
		);
	}

	@Test
	public void testAbstractClassAssociation(SessionFactoryScope scope) {
		final StatisticsImplementor stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		assert scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( RoleEntity.class )
				.getBytecodeEnhancementMetadata()
				.isEnhancedForLazyLoading();

		scope.inTransaction(
				session -> {
					final String qry = "select e from RoleEntity e";
					final List<RoleEntity> keyRoleEntities = session.createQuery( qry, RoleEntity.class ).list();

					assertThat( stats.getPrepareStatementCount(), is( 1L ) );

					for ( RoleEntity keyRoleEntity : keyRoleEntities ) {
						Object key = Hibernate.unproxy( keyRoleEntity.getKey() );
						assertThat( stats.getPrepareStatementCount(), is( 2L ) );

						Set<SpecializedEntity> specializedEntities = ( (SpecializedKey) key )
								.getSpecializedEntities();

						assertThat( stats.getPrepareStatementCount(), is( 2L ) );

						Iterator<SpecializedEntity> iterator = specializedEntities.iterator();
						while ( iterator.hasNext() ) {
							SpecializedEntity specializedEntity = iterator.next();
							assertThat( specializedEntity.getId(), notNullValue() );
							specializedEntity.getValue();
						}

						// but regardless there should not be an additional query
						assertThat( stats.getPrepareStatementCount(), is( 3L ) );
					}
				}
		);
	}

	@Test
	public void testNonAbstractAssociationWithSubclassValue(SessionFactoryScope scope) {
		final StatisticsImplementor stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		scope.inTransaction(
				session -> {
					final String qry = "select e from RoleEntity e";
					final List<RoleEntity> keyRoleEntities = session.createQuery( qry, RoleEntity.class ).list();

					assertThat( stats.getPrepareStatementCount(), is( 1L ) );

					assertThat( keyRoleEntities.size(), is( 1 ) );

					RoleEntity roleEntity = keyRoleEntities.get( 0 );
					assertThat(
							Hibernate.unproxy( roleEntity.getKey() ).getClass().getName(),
							is( SpecializedKey.class.getName() )
					);

					assertThat(
							Hibernate.unproxy( roleEntity.getSpecializedKey() ).getClass().getName(),
							is( MoreSpecializedKey.class.getName() )
					);
				}
		);
	}

	@Test
	public void testQueryAndDeleteDEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<DEntity> result = session.createQuery(
							"select d from D d ",
							DEntity.class
					).list();
					result.forEach( entity -> {
						session.remove( entity );
						session.remove( entity.getE() );
						session.remove( entity.getA() );
						Set<BEntity> bs = entity.getBs();
						bs.forEach( bEntity -> session.remove( bEntity ) );
						session.remove( entity.getC() );
						session.remove( entity.getG() );

					} );
				}
		);
	}

	@Test
	public void testLoadAndDeleteDEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					DEntity entity = session.getReference( DEntity.class, 1L );
					session.remove( entity );
					session.remove( entity.getE() );
					session.remove( entity.getA() );
					Set<BEntity> bs = entity.getBs();
					bs.forEach( bEntity -> session.remove( bEntity ) );
					session.remove( entity.getC() );
					session.remove( entity.getG() );
				}
		);
	}

	@Test
	public void testGetAndDeleteDEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					DEntity entity = session.get( DEntity.class, 1L );
					session.remove( entity );
					session.remove( entity.getE() );
					session.remove( entity.getA() );
					Set<BEntity> bs = entity.getBs();
					bs.forEach( bEntity -> session.remove( bEntity ) );
					session.remove( entity.getC() );
					session.remove( entity.getG() );
				}
		);
	}

	@Test
	public void testGetAndDeleteEEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EEntity entity = session.get( EEntity.class, 17L );
					session.remove( entity );
					session.remove( entity.getD() );
				}
		);
	}

	@Test
	public void testLoadAndDeleteEEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EEntity entity = session.getReference( EEntity.class, 17L );
					session.remove( entity );
					session.remove( entity.getD() );
				}
		);
	}

	@Test
	public void testQueryAndDeleteEEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<EEntity> result = session.createQuery(
							"select e from E e",
							EEntity.class
					).list();
					result.forEach( entity -> {
						session.remove( entity );
						session.remove( entity.getD() );
					} );
				}
		);
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					DEntity d = new DEntity();
					d.setD( "bla" );
					d.setLazyString( "I am lazy" );
					d.setNonLazyString( "I am not lazy" );
					d.setLazyStringBlobGroup( "I am lazy as a blob" );
					d.setOid( 1 );

					byte[] lBytes = "agdfagdfagfgafgsfdgasfdgfgasdfgadsfgasfdgasfdgasdasfdg".getBytes();
					Blob lBlob = session.getLobCreator().createBlob( lBytes );
					d.setBlob( lBlob );

					BEntity b1 = new BEntity();
					b1.setOid( 1 );
					b1.setB1( 34 );
					b1.setB2( "huhu" );

					BEntity b2 = new BEntity();
					b2.setOid( 2 );
					b2.setB1( 37 );
					b2.setB2( "haha" );

					Set<BEntity> lBs = new HashSet<>();
					lBs.add( b1 );
					lBs.add( b2 );
					d.setBs( lBs );

					AEntity a = new AEntity();
					a.setOid( 1 );
					a.setA( "hihi" );
					d.setA( a );

					EEntity e = new EEntity();
					e.setOid( 17 );
					e.setE1( "Balu" );
					e.setE2( "Bär" );

					e.setD( d );
					d.setE( e );

					CEntity c = new CEntity();
					c.setOid( 1 );
					c.setC1( "ast" );
					c.setC2( "qwert" );
					c.setC3( "yxcv" );
					d.setC( c );

					GEntity g = new GEntity();
					g.setOid( 1 );
					g.getdEntities().add( d );
					d.setG( g );


					session.persist( b1 );
					session.persist( b2 );
					session.persist( a );
					session.persist( c );
					session.persist( g );
					session.persist( d );
					session.persist( e );


					// create a slew of Activity objects, some with Instruction reference
					// some without.

					for ( int i = 0; i < 30; i++ ) {
						final Activity activity = new Activity( i, "Activity #" + i, null );
						if ( i % 2 == 0 ) {
							final Instruction instr = new Instruction( i, "Instruction #" + i );
							activity.setInstruction( instr );
							session.persist( instr );
						}
						else {
							final WebApplication webApplication = new WebApplication( i, "http://" + i + ".com" );
							webApplication.setName( "name #" + i );
							activity.setWebApplication( webApplication );
							webApplication.getActivities().add( activity );
							session.persist( webApplication );
						}

						session.persist( activity );
					}

					RoleEntity roleEntity = new RoleEntity();
					roleEntity.setOid( 1L );

					SpecializedKey specializedKey = new SpecializedKey();
					specializedKey.setOid( 1L );

					MoreSpecializedKey moreSpecializedKey = new MoreSpecializedKey();
					moreSpecializedKey.setOid( 3L );

					SpecializedEntity specializedEntity = new SpecializedEntity();
					specializedEntity.setId( 2L );
					specializedKey.addSpecializedEntity( specializedEntity );
					specializedEntity.setSpecializedKey( specializedKey );

					specializedKey.addRole( roleEntity );
					roleEntity.setKey( specializedKey );
					roleEntity.setSpecializedKey( moreSpecializedKey );
					moreSpecializedKey.addRole( roleEntity );
					session.persist( specializedEntity );
					session.persist( roleEntity );
					session.persist( specializedKey );
					session.persist( moreSpecializedKey );
				}
		);
	}

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@MappedSuperclass
	public static class BaseEntity {
		@Id
		private long oid;
		private short version;

		public long getOid() {
			return oid;
		}

		public void setOid(long oid) {
			this.oid = oid;
		}

		public short getVersion() {
			return version;
		}

		public void setVersion(short version) {
			this.version = version;
		}
	}

	@Entity(name = "A")
	@Table(name = "A")
	public static class AEntity extends BaseEntity {
		@Column(name = "A")
		private String a;

		public String getA() {
			return a;
		}

		public void setA(String a) {
			this.a = a;
		}
	}


	@Entity(name = "B")
	@Table(name = "B")
	public static class BEntity extends BaseEntity {
		private Integer b1;
		private String b2;

		public Integer getB1() {
			return b1;
		}

		public void setB1(Integer b1) {
			this.b1 = b1;
		}

		public String getB2() {
			return b2;
		}

		public void setB2(String b2) {
			this.b2 = b2;
		}
	}


	@Entity(name = "C")
	@Table(name = "C")
	public static class CEntity extends BaseEntity {
		private String c1;
		private String c2;
		private String c3;
		private Long c4;

		public String getC1() {
			return c1;
		}

		public void setC1(String c1) {
			this.c1 = c1;
		}

		public String getC2() {
			return c2;
		}

		@Basic(fetch = FetchType.LAZY)
		public void setC2(String c2) {
			this.c2 = c2;
		}

		public String getC3() {
			return c3;
		}

		public void setC3(String c3) {
			this.c3 = c3;
		}

		public Long getC4() {
			return c4;
		}

		public void setC4(Long c4) {
			this.c4 = c4;
		}
	}

	@Entity(name = "D")
	@Table(name = "D")
	public static class DEntity extends BaseEntity {
		private String d;

		// ****** Relations *****************
		@OneToOne(fetch = FetchType.LAZY)
		@LazyGroup("a")
		public AEntity a;

		@OneToOne(fetch = FetchType.LAZY)
		@LazyGroup("c")
		public CEntity c;

		@OneToMany(targetEntity = BEntity.class)
		public Set<BEntity> bs;

		@OneToOne(mappedBy = "d", fetch = FetchType.LAZY)
		@LazyGroup("e")
		private EEntity e;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn()
		@LazyGroup("g")
		public GEntity g;

		private String nonLazyString;

		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("lazyString")
		private String lazyString;

		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("blob")
		private String lazyStringBlobGroup;

		@Lob
		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("blob")
		@Column(name = "blob_field")
		private Blob blob;

		public String getD() {
			return d;
		}

		public void setD(String d) {
			this.d = d;
		}


		public AEntity getA() {
			return a;
		}

		public void setA(AEntity a) {
			this.a = a;
		}

		public Set<BEntity> getBs() {
			return bs;
		}

		public void setBs(Set<BEntity> bs) {
			this.bs = bs;
		}

		public CEntity getC() {
			return c;
		}

		public void setC(CEntity c) {
			this.c = c;
		}

		public Blob getBlob() {
			return blob;
		}

		public void setBlob(Blob blob) {
			this.blob = blob;
		}

		public EEntity getE() {
			return e;
		}

		public void setE(EEntity e) {
			this.e = e;
		}

		public GEntity getG() {
			return g;
		}

		public void setG(GEntity g) {
			this.g = g;
		}

		public String getNonLazyString() {
			return nonLazyString;
		}

		public void setNonLazyString(String nonLazyString) {
			this.nonLazyString = nonLazyString;
		}

		public String getLazyString() {
			return lazyString;
		}

		public void setLazyString(String lazyString) {
			this.lazyString = lazyString;
		}

		public String getLazyStringBlobGroup() {
			return lazyStringBlobGroup;
		}

		public void setLazyStringBlobGroup(String lazyStringBlobGroup) {
			this.lazyStringBlobGroup = lazyStringBlobGroup;
		}
	}

	@Entity(name = "E")
	@Table(name = "E")
	public static class EEntity extends BaseEntity {
		private String e1;
		private String e2;

		@OneToOne(fetch = FetchType.LAZY)
		private DEntity d;

		public String getE1() {
			return e1;
		}

		public void setE1(String e1) {
			this.e1 = e1;
		}

		public String getE2() {
			return e2;
		}

		public void setE2(String e2) {
			this.e2 = e2;
		}

		public DEntity getD() {
			return d;
		}

		public void setD(DEntity d) {
			this.d = d;
		}
	}

	@Entity(name = "G")
	@Table(name = "G")
	public static class GEntity extends BaseEntity {

		@OneToMany(mappedBy = "g")
		public Set<DEntity> dEntities = new HashSet<>();

		public Set<DEntity> getdEntities() {
			return dEntities;
		}

		public void setdEntities(Set<DEntity> dEntities) {
			this.dEntities = dEntities;
		}
	}

}
