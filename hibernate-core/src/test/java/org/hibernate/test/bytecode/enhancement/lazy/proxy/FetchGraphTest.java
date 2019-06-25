/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import java.sql.Blob;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.ScrollableResults;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings({"unused", "WeakerAccess","ResultOfMethodCallIgnored"})
@TestForIssue( jiraKey = "HHH-11147" )
@RunWith( BytecodeEnhancerRunner.class )
@EnhancementOptions( lazyLoading = true )
public class FetchGraphTest extends BaseNonConfigCoreFunctionalTestCase {


	@Test
	public void testLoadNonOwningOneToOne() {
		// Test loading D and accessing E
		// 		E is the owner of the FK, not D.  When `D#e` is accessed we
		//		need to actually load E because its table has the FK value, not
		//		D's table

		final StatisticsImplementor stats = sessionFactory().getStatistics();
		stats.clear();

		assert sessionFactory().getMetamodel()
				.entityPersister( DEntity.class )
				.getBytecodeEnhancementMetadata()
				.isEnhancedForLazyLoading();

		inSession(
				session -> {
					final DEntity entityD = session.load( DEntity.class, 1L );
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
	public void testLoadOwningOneToOne() {
		// switch it around

		final StatisticsImplementor stats = sessionFactory().getStatistics();
		stats.clear();

		assert sessionFactory().getMetamodel()
				.entityPersister( DEntity.class )
				.getBytecodeEnhancementMetadata()
				.isEnhancedForLazyLoading();

		inSession(
				session -> {
					final EEntity entityE = session.load( EEntity.class, 17L );
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
	public void testFetchingScroll() {
		final StatisticsImplementor stats = sessionFactory().getStatistics();
		stats.clear();

		assert sessionFactory().getMetamodel()
				.entityPersister( DEntity.class )
				.getBytecodeEnhancementMetadata()
				.isEnhancedForLazyLoading();


		inStatelessSession(
				session -> {
					final String qry = "select e from E e join fetch e.d";

					final ScrollableResults scrollableResults = session.createQuery( qry ).scroll();
					while ( scrollableResults.next() ) {
						System.out.println( "Got entity : " + scrollableResults.get( 0 ) );
					}
				}
		);

		inStatelessSession(
				session -> {
					final String qry = "select d from D d " +
							"join fetch d.a " +
							"join fetch d.bs " +
							"join fetch d.c " +
							"join fetch d.e " +
							"join fetch d.g";

					final ScrollableResults scrollableResults = session.createQuery( qry ).scroll();
					while ( scrollableResults.next() ) {
						System.out.println( "Got entity : " + scrollableResults.get( 0 ) );
					}
				}
		);

		inStatelessSession(
				session -> {
					final String qry = "select g from G g join fetch g.dEntities";

					final ScrollableResults scrollableResults = session.createQuery( qry ).scroll();
					while ( scrollableResults.next() ) {
						System.out.println( "Got entity : " + scrollableResults.get( 0 ) );
					}
				}
		);
	}


	@Test
	public void testLazyAssociationSameAsNonLazyInPC() {

		assert sessionFactory().getMetamodel()
				.entityPersister( DEntity.class )
				.getInstrumentationMetadata()
				.isEnhancedForLazyLoading();

		inSession(
				session -> {
					final AEntity entityA = session.get( AEntity.class, 1L );

					final DEntity entityD = session.load( DEntity.class, 1L );
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
	public void testRandomAccess() {
		final StatisticsImplementor stats = sessionFactory().getStatistics();
		stats.clear();

		final EntityPersister persister = sessionFactory().getMetamodel().entityPersister( DEntity.class );
		assert persister.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();

		inSession(
				session -> {
					// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					// Load a D

					final DEntity entityD = session.load( DEntity.class, 1L );

					assertThat( entityD instanceof HibernateProxy, is(false) );
					assertThat( entityD instanceof PersistentAttributeInterceptable, is(true) );
					assertThat( Hibernate.isInitialized( entityD ), is(false) );
					// Because D is enhanced we should not have executed any SQL
					assertThat( stats.getPrepareStatementCount(), is( 0L ) );

					// access the id.
					// 		-since entityD is a "enhanced proxy", this should not trigger loading
					assertThat( entityD.getOid(), is(1L) );
					assertThat( Hibernate.isInitialized( entityD ), is(false) );
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

					assertThat( entityD.getE().getOid(), is(17L) );


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
	public void testNullManyToOneHql() {
		final StatisticsImplementor stats = sessionFactory().getStatistics();
		stats.clear();

		inTransaction(
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
							assertThat( stats.getPrepareStatementCount(), is( ++expectedCount  ) );
						}

						if ( activity.getWebApplication() != null ) {
							// trigger base group initialization
							activity.getWebApplication().getName();
							assertThat( stats.getPrepareStatementCount(), is( ++expectedCount  ) );
							// trigger  initialization
							activity.getWebApplication().getSiteUrl();
							assertThat( stats.getPrepareStatementCount(), is( ++expectedCount  ) );
						}
					}
				}
		);
	}

	@Test
	public void testAbstractClassAssociation() {
		final StatisticsImplementor stats = sessionFactory().getStatistics();
		stats.clear();

		assert sessionFactory().getMetamodel()
				.entityPersister( RoleEntity.class )
				.getInstrumentationMetadata()
				.isEnhancedForLazyLoading();

		inTransaction(
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
	public void testNonAbstractAssociationWithSubclassValue() {
		final StatisticsImplementor stats = sessionFactory().getStatistics();
		stats.clear();

		inTransaction(
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
	public void testQueryAndDeleteDEntity() {
		inTransaction(
				session -> {
					List<DEntity> result = session.createQuery(
							"select d from D d ",
							DEntity.class
					).list();
					result.forEach( entity -> {
						session.delete( entity );
						session.delete( entity.getE() );
						session.delete( entity.getA() );
						Set<BEntity> bs = entity.getBs();
						bs.forEach( bEntity -> session.delete( bEntity ) );
						session.delete( entity.getC() );
						session.delete( entity.getG() );

					} );
				}
		);
	}

	@Test
	public void testLoadAndDeleteDEntity() {
		inTransaction(
				session -> {
					DEntity entity = session.load( DEntity.class, 1L );
					session.delete( entity );
					session.delete( entity.getE() );
					session.delete( entity.getA() );
					Set<BEntity> bs = entity.getBs();
					bs.forEach( bEntity -> session.delete( bEntity ) );
					session.delete( entity.getC() );
					session.delete( entity.getG() );
				}
		);
	}

	@Test
	public void testGetAndDeleteDEntity() {
		inTransaction(
				session -> {
					DEntity entity = session.get( DEntity.class, 1L );
					session.delete( entity );
					session.delete( entity.getE() );
					session.delete( entity.getA() );
					Set<BEntity> bs = entity.getBs();
					bs.forEach( bEntity -> session.delete( bEntity ) );
					session.delete( entity.getC() );
					session.delete( entity.getG() );
				}
		);
	}

	@Test
	public void testGetAndDeleteEEntity() {
		inTransaction(
				session -> {
					EEntity entity = session.get( EEntity.class, 17L );
					session.delete( entity );
					session.delete( entity.getD() );
				}
		);
	}

	@Test
	public void testLoadAndDeleteEEntity() {
		inTransaction(
				session -> {
					EEntity entity = session.load( EEntity.class, 17L );
					session.delete( entity );
					session.delete( entity.getD() );
				}
		);
	}

	@Test
	public void testQueryAndDeleteEEntity() {
		inTransaction(
				session -> {
					List<EEntity> result = session.createQuery(
							"select e from E e",
							EEntity.class
					).list();
					result.forEach( entity -> {
						session.delete( entity );
						session.delete( entity.getD() );
					} );
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
		sources.addAnnotatedClass( AEntity.class );
		sources.addAnnotatedClass( BEntity.class );
		sources.addAnnotatedClass( CEntity.class );
		sources.addAnnotatedClass( DEntity.class );
		sources.addAnnotatedClass( EEntity.class );
		sources.addAnnotatedClass( GEntity.class );

		sources.addAnnotatedClass( Activity.class );
		sources.addAnnotatedClass( Instruction.class );
		sources.addAnnotatedClass( WebApplication.class );

		sources.addAnnotatedClass( SpecializedKey.class );
		sources.addAnnotatedClass( MoreSpecializedKey.class );
		sources.addAnnotatedClass( RoleEntity.class );
		sources.addAnnotatedClass( AbstractKey.class );
		sources.addAnnotatedClass( GenericKey.class );
		sources.addAnnotatedClass( SpecializedEntity.class );
	}

	@Before
	public void prepareTestData() {
		inTransaction(
				session -> {
					DEntity d = new DEntity();
					d.setD( "bla" );
					d.setOid( 1 );

					byte[] lBytes = "agdfagdfagfgafgsfdgasfdgfgasdfgadsfgasfdgasfdgasdasfdg".getBytes();
					Blob lBlob = Hibernate.getLobCreator( session ).createBlob( lBytes );
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


					session.save( b1 );
					session.save( b2 );
					session.save( a );
					session.save( c );
					session.save( g );
					session.save( d );
					session.save( e );


					// create a slew of Activity objects, some with Instruction reference
					// some without.

					for ( int i = 0; i < 30; i++ ) {
						final Activity activity = new Activity( i, "Activity #" + i, null );
						if ( i % 2 == 0 ) {
							final Instruction instr = new Instruction( i, "Instruction #" + i );
							activity.setInstruction( instr );
							session.save( instr );
						}
						else {
							final WebApplication webApplication = new WebApplication( i, "http://" + i + ".com" );
							webApplication.setName( "name #" + i );
							activity.setWebApplication( webApplication );
							webApplication.getActivities().add( activity );
							session.save( webApplication );
						}

						session.save( activity );
					}

					RoleEntity roleEntity = new RoleEntity();
					roleEntity.setOid( 1L );

					SpecializedKey specializedKey = new SpecializedKey();
					specializedKey.setOid(1L);

					MoreSpecializedKey moreSpecializedKey = new MoreSpecializedKey();
					moreSpecializedKey.setOid( 3L );

					SpecializedEntity specializedEntity = new SpecializedEntity();
					specializedEntity.setId( 2L );
					specializedKey.addSpecializedEntity( specializedEntity );
					specializedEntity.setSpecializedKey( specializedKey);

					specializedKey.addRole( roleEntity );
					roleEntity.setKey( specializedKey );
					roleEntity.setSpecializedKey( moreSpecializedKey );
					moreSpecializedKey.addRole( roleEntity );
					session.save( specializedEntity );
					session.save( roleEntity );
					session.save( specializedKey );
					session.save( moreSpecializedKey );
				}
		);
	}

	@After
	public void cleanUpTestData() {
		inTransaction(
				session -> {
					session.createQuery( "delete from E" ).executeUpdate();
					session.createQuery( "delete from D" ).executeUpdate();
					session.createQuery( "delete from C" ).executeUpdate();
					session.createQuery( "delete from B" ).executeUpdate();
					session.createQuery( "delete from A" ).executeUpdate();
					session.createQuery( "delete from G" ).executeUpdate();

					session.createQuery( "delete from Activity" ).executeUpdate();
					session.createQuery( "delete from Instruction" ).executeUpdate();
					session.createQuery( "delete from WebApplication" ).executeUpdate();

					session.createQuery( "delete from SpecializedEntity" ).executeUpdate();
					session.createQuery( "delete from RoleEntity" ).executeUpdate();
					session.createQuery( "delete from MoreSpecializedKey" ).executeUpdate();
					session.createQuery( "delete from SpecializedKey" ).executeUpdate();
					session.createQuery( "delete from GenericKey" ).executeUpdate();
					session.createQuery( "delete from AbstractKey" ).executeUpdate();
				}
		);
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
//		@LazyToOne(LazyToOneOption.PROXY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		@LazyGroup("a")
		public AEntity a;

		@OneToOne(fetch = FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
//		@LazyToOne(LazyToOneOption.PROXY)
		@LazyGroup("c")
		public CEntity c;

		@OneToMany(targetEntity = BEntity.class)
		public Set<BEntity> bs;

		@OneToOne(mappedBy = "d", fetch = FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		@LazyGroup("e")
		private EEntity e;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn()
		@LazyGroup("g")
		public GEntity g;

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
