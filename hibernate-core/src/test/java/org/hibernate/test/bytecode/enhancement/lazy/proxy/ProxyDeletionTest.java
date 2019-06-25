/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import java.sql.Blob;
import java.util.HashSet;
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
import org.hibernate.annotations.LazyGroup;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class ProxyDeletionTest extends BaseNonConfigCoreFunctionalTestCase {


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

		@OneToOne(fetch = FetchType.LAZY)
		public AEntity a;

		@OneToOne(fetch = FetchType.LAZY)
		public CEntity c;

		@OneToMany(targetEntity = BEntity.class)
		public Set<BEntity> bs;

		@OneToOne(mappedBy = "d", fetch = FetchType.LAZY)
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
