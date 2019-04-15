/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.group;

import java.sql.Blob;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-11223" )
@RunWith( BytecodeEnhancerRunner.class )
@CustomEnhancementContext( EnhancerTestContext.class )
@FailureExpected( jiraKey = "HHH-11223" )
public class ComplexFetchGroupTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testNonOwningOneToOneAccess() {
		final StatisticsImplementor stats = sessionFactory().getStatistics();
		stats.clear();

		inSession(
				session -> {
					final SessionStatistics sessionStats = session.getStatistics();


					final DEntity entityD = session.load( DEntity.class, 1L );
					// todo : really this should be 0
					assertThat( stats.getPrepareStatementCount(), is( 1L ) );

					entityD.getC();
					assertThat( stats.getPrepareStatementCount(), is( 2L ) );
					entityD.getC();
					assertThat( stats.getPrepareStatementCount(), is( 2L ) );

					final EEntity e1 = entityD.getE();
					assertThat( stats.getPrepareStatementCount(), is( 3L ) );

					final EEntity e2 = entityD.getE();
					assertThat( stats.getPrepareStatementCount(), is( 3L ) );
				}
		);
	}

	@Test
	public void testOwningOneToOneAccess() {
		final StatisticsImplementor stats = sessionFactory().getStatistics();
		stats.clear();

		inSession(
				session -> {
					final SessionStatistics sessionStats = session.getStatistics();

					final EEntity entityE = session.load( EEntity.class, 17L );
					assertThat( stats.getPrepareStatementCount(), is( 1L ) );

//					final DEntity d1 = entityE.getD();
//					assertThat( stats.getPrepareStatementCount(), is( 1 ) );
//
//					final DEntity d2 = entityE.getD();
//					assertThat( stats.getPrepareStatementCount(), is( 1 ) );
				}
		);
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
	}

	@Before
	public void prepareTestData() {
		inTransaction(
				session -> {
					DEntity d = new DEntity();
					d.setD("bla");
					d.setOid(1);

					byte[] lBytes = "agdfagdfagfgafgsfdgasfdgfgasdfgadsfgasfdgasfdgasdasfdg".getBytes();
					Blob lBlob = Hibernate.getLobCreator( session).createBlob( lBytes);
					d.setBlob(lBlob);

					BEntity b1 = new BEntity();
					b1.setOid(1);
					b1.setB1(34);
					b1.setB2("huhu");

					BEntity b2 = new BEntity();
					b2.setOid(2);
					b2.setB1(37);
					b2.setB2("haha");

					Set<BEntity> lBs = new HashSet<>();
					lBs.add(b1);
					lBs.add(b2);
					d.setBs(lBs);

					AEntity a = new AEntity();
					a.setOid(1);
					a.setA("hihi");
					d.setA(a);

					EEntity e = new EEntity();
					e.setOid(17);
					e.setE1("Balu");
					e.setE2("Bär");

					e.setD( d );
					d.setE( e );

					CEntity c = new CEntity();
					c.setOid(1);
					c.setC1("ast");
					c.setC2("qwert");
					c.setC3("yxcv");
					d.setC(c);

					session.save(b1);
					session.save(b2);
					session.save(a);
					session.save(c);
					session.save(d);
					session.save(e);
				}
		);
	}


	@Entity( name = "A")
	@Table(name="A")
	public static class AEntity {
		@Id
		private long oid;

		@Column(name="A")
		private String a;

		public String getA() {
			return a;
		}

		public void setA(String a) {
			this.a = a;
		}

		public long getOid() {
			return oid;
		}

		public void setOid(long oid) {
			this.oid = oid;
		}
	}


	@Entity( name = "B" )
	@Table(name="B")
	public static class BEntity {
		@Id
		public long oid;
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

		public long getOid() {
			return oid;
		}

		public void setOid(long oid) {
			this.oid = oid;
		}
	}


	@Entity( name = "C" )
	@Table(name="C")
	public static class CEntity {

		@Id
		private long oid;
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

		public long getOid() {
			return oid;
		}

		public void setOid(long oid) {
			this.oid = oid;
		}
	}

	@Entity( name = "D")
	@Table(name="D")
	public static class DEntity {

		// ****** ID *****************
		@Id
		private long oid;
		private String d;
		// ****** Relations *****************
		@OneToOne(fetch = FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		@LazyGroup("a")
		public AEntity a;

		@OneToOne(fetch = FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		@LazyGroup("c")
		public CEntity c;
		@OneToMany(targetEntity = BEntity.class)
		public Set<BEntity> bs;

		@OneToOne(mappedBy="d", fetch = FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		@LazyGroup("e")
		private EEntity e;

		@Lob
		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("blob")
		private Blob blob;

		public String getD() {
			return d;
		}

		public void setD(String d) {
			this.d = d;
		}


		// ****** ID *****************
		public long getOid() {
			return oid;
		}

		public void setOid(long oid) {
			this.oid = oid;
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
	}

	@Entity( name = "E" )
	@Table(name="E")
	public static class EEntity {
		@Id
		private long oid;
		private String e1;
		private String e2;

		@OneToOne(fetch=FetchType.LAZY)
		private DEntity d;

		public long getOid() {
			return oid;
		}
		public void setOid(long oid) {
			this.oid = oid;
		}
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
}
