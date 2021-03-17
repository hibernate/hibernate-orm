/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.graphs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityGraph;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TypedQuery;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 * @author Nathan Xu
 */
public class LoadAndFetchGraphTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				AEntity.class,
				BEntity.class,
				CEntity.class,
				DEntity.class,
				EEntity.class
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( Environment.GENERATE_STATISTICS, "true" );
		super.addConfigOptions( options );
	}

	@Before
	public void setUp() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			// Create the model twice, with different IDs,
			// because we also need to test what happens when multiple results are loaded by a query.
			for ( int offset : new int[]{ 0, 10000 } ) {
				AEntity a1 = new AEntity();
				a1.setId( offset + 1 );
				a1.setLabel( "A1" );

				AEntity a2 = new AEntity();
				a2.setId( offset + 2 );
				a2.setLabel( "A2" );

				entityManager.persist( a1 );
				entityManager.persist( a2 );

				BEntity b1 = new BEntity();
				b1.setId( offset + 1 );
				b1.setLabel( "B1" );

				BEntity b2 = new BEntity();
				b2.setId( offset + 2 );
				b2.setLabel( "B2" );

				entityManager.persist( b1 );
				entityManager.persist( b2 );

				EEntity e1 = new EEntity();
				e1.setId( offset + 1 );
				e1.setLabel( "E1" );

				EEntity e2 = new EEntity();
				e2.setId( offset + 2 );
				e2.setLabel( "E2" );

				EEntity e3 = new EEntity();
				e3.setId( offset + 3 );
				e3.setLabel( "E3" );

				EEntity e4 = new EEntity();
				e4.setId( offset + 4 );
				e4.setLabel( "E4" );

				entityManager.persist( e1 );
				entityManager.persist( e2 );
				entityManager.persist( e3 );
				entityManager.persist( e4 );

				DEntity d1 = new DEntity();
				d1.setId( offset + 1 );
				d1.setLabel( "D1" );
				d1.setE( e1 );

				DEntity d2 = new DEntity();
				d2.setId( offset + 2 );
				d2.setLabel( "D2" );
				d2.setE( e2 );

				CEntity c1 = new CEntity();
				c1.setId( offset + 1 );
				c1.setLabel( "C1" );
				c1.setA( a1 );
				c1.setB( b1 );
				c1.addD( d1 );
				c1.addD( d2 );

				entityManager.persist( c1 );

				DEntity d3 = new DEntity();
				d3.setId( offset + 3 );
				d3.setLabel( "D3" );
				d3.setE( e3 );

				DEntity d4 = new DEntity();
				d4.setId( offset + 4 );
				d4.setLabel( "D4" );
				d4.setE( e4 );

				CEntity c2 = new CEntity();
				c2.setId( offset + 2 );
				c2.setLabel( "C2" );
				c2.setA( a2 );
				c2.setB( b2 );
				c2.addD( d3 );
				c2.addD( d4 );

				entityManager.persist( c2 );

				CEntity c3 = new CEntity();
				c3.setId( offset + 3 );
				c3.setLabel( "C3" );

				entityManager.persist( c3 );

				CEntity c4 = new CEntity();
				c4.setId( offset + 4 );
				c4.setLabel( "C4" );

				entityManager.persist( c4 );

				c1.setC( c2 );
				c2.setC( c3 );
				c1.setEagerC( c4 );

				int id = 5;
				for ( int i = 0; i < 10; i++ ) {
					DEntity dn = new DEntity();
					dn.setId( offset + id++ );
					dn.setLabel( "label" );
					dn.setE( e3 );
					entityManager.persist( dn );
				}
			}
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14097")
	public void testQueryById() {
		Statistics statistics = entityManagerFactory().unwrap( SessionFactory.class ).getStatistics();
		statistics.clear();
		doInJPA(
				this::entityManagerFactory, entityManager -> {
					TypedQuery<CEntity> query = entityManager.createQuery(
							"select c from CEntity as c where c.id = :cid ",
							CEntity.class
					);
					query.setParameter( "cid", 1 );
					CEntity cEntity = query.getSingleResult();

					assertFalse( Hibernate.isInitialized( cEntity.getA() ) );
					assertFalse( Hibernate.isInitialized( cEntity.getB() ) );
					assertFalse( Hibernate.isInitialized( cEntity.getC() ) );
					assertFalse( Hibernate.isInitialized( cEntity.getdList() ) );

					assertTrue( Hibernate.isInitialized( cEntity.getEagerC() ) );

					// 1 + 1 for the eager C
					assertEquals( 2L, statistics.getPrepareStatementCount() );
				} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14097")
	public void testQueryByIdWithLoadGraph() {
		Statistics statistics = entityManagerFactory().unwrap( SessionFactory.class ).getStatistics();
		statistics.clear();
		doInJPA(
				this::entityManagerFactory, entityManager -> {
					EntityGraph<CEntity> entityGraph = entityManager.createEntityGraph( CEntity.class );
					entityGraph.addAttributeNodes( "a", "b" );
					entityGraph.addSubgraph( "dList" ).addAttributeNodes( "e" );

					TypedQuery<CEntity> query = entityManager.createQuery(
							"select c from CEntity as c where c.id = :cid ",
							CEntity.class
					);
					query.setHint( GraphSemantic.LOAD.getJpaHintName(), entityGraph );
					query.setParameter( "cid", 1 );

					CEntity cEntity = query.getSingleResult();

					assertTrue( Hibernate.isInitialized( cEntity.getA() ) );
					assertTrue( Hibernate.isInitialized( cEntity.getB() ) );
					assertFalse( Hibernate.isInitialized( cEntity.getC() ) );
					assertTrue( Hibernate.isInitialized( cEntity.getdList() ) );
					cEntity.getdList().forEach( dEntity -> {
						assertTrue( Hibernate.isInitialized( dEntity.getE() ) );
					} );

					// With LOAD semantic, attributes that are not mentioned in the graph are LAZY or EAGER,
					// depending on the mapping.
					assertTrue( Hibernate.isInitialized( cEntity.getEagerC() ) );

					// 1 + 1 for the eager C
					assertEquals( 2L, statistics.getPrepareStatementCount() );
				} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14097")
	public void testQueryByIdWithFetchGraph() {
		Statistics statistics = entityManagerFactory().unwrap( SessionFactory.class ).getStatistics();
		statistics.clear();
		doInJPA(
				this::entityManagerFactory, entityManager -> {
					EntityGraph<CEntity> entityGraph = entityManager.createEntityGraph( CEntity.class );
					entityGraph.addAttributeNodes( "a", "b" );
					entityGraph.addSubgraph( "dList" ).addAttributeNodes( "e" );

					TypedQuery<CEntity> query = entityManager.createQuery(
							"select c from CEntity as c where c.id = :cid ",
							CEntity.class
					);
					query.setHint( GraphSemantic.FETCH.getJpaHintName(), entityGraph );
					query.setParameter( "cid", 1 );

					CEntity cEntity = query.getSingleResult();
					assertTrue( Hibernate.isInitialized( cEntity.getA() ) );
					assertTrue( Hibernate.isInitialized( cEntity.getB() ) );
					assertFalse( Hibernate.isInitialized( cEntity.getC() ) );
					assertTrue( Hibernate.isInitialized( cEntity.getdList() ) );
					cEntity.getdList().forEach( dEntity -> {
						assertTrue( Hibernate.isInitialized( dEntity.getE() ) );
					} );

					// With FETCH semantic, attributes that are not mentioned in the graph are LAZY,
					// even if they were EAGER in the mapping.
					assertFalse( Hibernate.isInitialized( cEntity.getEagerC() ) );

					assertEquals( 1L, statistics.getPrepareStatementCount() );
				} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14097")
	public void testQueryByIdWithFetchGraph2() {
		Statistics statistics = entityManagerFactory().unwrap( SessionFactory.class ).getStatistics();
		statistics.clear();
		doInJPA(
				this::entityManagerFactory, entityManager -> {
					EntityGraph<CEntity> entityGraph = entityManager.createEntityGraph( CEntity.class );
					entityGraph.addSubgraph( "c" ).addAttributeNodes( "a" );

					TypedQuery<CEntity> query = entityManager.createQuery(
							"select c from CEntity as c where c.id = :cid ",
							CEntity.class
					);
					query.setHint( GraphSemantic.FETCH.getJpaHintName(), entityGraph );
					query.setParameter( "cid", 1 );

					CEntity cEntity = query.getSingleResult();
					assertTrue( Hibernate.isInitialized( cEntity.getC() ) );
					assertTrue( Hibernate.isInitialized( cEntity.getC().getA() ) );
					assertFalse( Hibernate.isInitialized( cEntity.getC().getC() ) );

					// With FETCH semantic, attributes that are not mentioned in the graph are LAZY,
					// even if they were EAGER in the mapping.
					assertFalse( Hibernate.isInitialized( cEntity.getEagerC() ) );

					assertEquals( 1L, statistics.getPrepareStatementCount() );
				} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14124")
	public void testQueryByIdWithLoadGraphMultipleResults() {
		Statistics statistics = entityManagerFactory().unwrap( SessionFactory.class ).getStatistics();
		statistics.clear();
		doInJPA( this::entityManagerFactory, entityManager -> {
			EntityGraph<CEntity> entityGraph = entityManager.createEntityGraph( CEntity.class );
			entityGraph.addAttributeNodes( "a", "b" );
			entityGraph.addSubgraph( "dList" ).addAttributeNodes( "e" );

			TypedQuery<CEntity> query = entityManager.createQuery(
					"select c from CEntity as c where c.id in :cid ",
					CEntity.class
			);
			query.setHint( GraphSemantic.LOAD.getJpaHintName(), entityGraph );
			query.setParameter( "cid", Arrays.asList( 1, 10001 ) );

			List<CEntity> cEntityList = query.getResultList();
			assertEquals( 2, cEntityList.size() );

			for ( CEntity cEntity : cEntityList ) {
				assertTrue( Hibernate.isInitialized( cEntity.getA() ) );
				assertTrue( Hibernate.isInitialized( cEntity.getB() ) );
				assertFalse( Hibernate.isInitialized( cEntity.getC() ) );
				assertTrue( Hibernate.isInitialized( cEntity.getdList() ) );
				cEntity.getdList().forEach( dEntity -> {
					assertTrue( Hibernate.isInitialized( dEntity.getE() ) );
				} );

				// With LOAD semantic, attributes that are not mentioned in the graph are LAZY or EAGER,
				// depending on the mapping.
				assertTrue( Hibernate.isInitialized( cEntity.getEagerC() ) );
			}

			// 1 + 2 for the eager C
			assertEquals( 3L, statistics.getPrepareStatementCount() );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14124")
	public void testQueryByIdWithFetchGraphMultipleResults() {
		Statistics statistics = entityManagerFactory().unwrap( SessionFactory.class ).getStatistics();
		statistics.clear();
		doInJPA( this::entityManagerFactory, entityManager -> {
			EntityGraph<CEntity> entityGraph = entityManager.createEntityGraph( CEntity.class );
			entityGraph.addAttributeNodes( "a", "b" );
			entityGraph.addSubgraph( "dList" ).addAttributeNodes( "e" );

			TypedQuery<CEntity> query = entityManager.createQuery(
					"select c from CEntity as c where c.id in :cid ",
					CEntity.class
			);
			query.setHint( GraphSemantic.FETCH.getJpaHintName(), entityGraph );
			query.setParameter( "cid", Arrays.asList( 1, 10001 ) );

			List<CEntity> cEntityList = query.getResultList();
			assertEquals( 2, cEntityList.size() );

			for ( CEntity cEntity : cEntityList ) {
				assertTrue( Hibernate.isInitialized( cEntity.getA() ) );
				assertTrue( Hibernate.isInitialized( cEntity.getB() ) );
				assertFalse( Hibernate.isInitialized( cEntity.getC() ) );
				assertTrue( Hibernate.isInitialized( cEntity.getdList() ) );
				cEntity.getdList().forEach( dEntity -> {
					assertTrue( Hibernate.isInitialized( dEntity.getE() ) );
				} );

				// With FETCH semantic, attributes that are not mentioned in the graph are LAZY,
				// even if they were EAGER in the mapping.
				assertFalse( Hibernate.isInitialized( cEntity.getEagerC() ) );
			}

			assertEquals( 1L, statistics.getPrepareStatementCount() );
		} );
	}

	@Entity(name = "AEntity")
	@Table(name = "A")
	public static class AEntity {

		@Id
		private Integer id;

		private String label;

		@OneToMany(
				fetch = FetchType.LAZY,
				mappedBy = "a",
				cascade = CascadeType.ALL,
				orphanRemoval = true
		)
		private List<CEntity> cList = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

	}

	@Entity(name = "BEntity")
	@Table(name = "B")
	public static class BEntity {

		@Id
		private Integer id;

		private String label;

		@OneToMany(
				fetch = FetchType.LAZY,
				mappedBy = "b",
				cascade = CascadeType.ALL,
				orphanRemoval = true
		)
		private List<CEntity> cList = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

	}

	@Entity(name = "CEntity")
	@Table(name = "C")
	public static class CEntity {

		@Id
		private Integer id;

		private String label;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "A_ID")
		private AEntity a;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "B_ID")
		private BEntity b;

		@ManyToOne(fetch = FetchType.LAZY)
		private CEntity c;

		@OneToOne(fetch = FetchType.EAGER)
		private CEntity eagerC;

		@OneToMany(
				fetch = FetchType.LAZY,
				mappedBy = "c",
				cascade = CascadeType.ALL,
				orphanRemoval = true
		)
		private List<DEntity> dList = new ArrayList<>();

		public void addD(DEntity d) {
			dList.add( d );
			d.setC( this );
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public AEntity getA() {
			return a;
		}

		public void setA(AEntity a) {
			this.a = a;
		}

		public BEntity getB() {
			return b;
		}

		public void setB(BEntity b) {
			this.b = b;
		}

		public CEntity getC() {
			return c;
		}

		public void setC(CEntity c) {
			this.c = c;
		}

		public CEntity getEagerC() {
			return eagerC;
		}

		public void setEagerC(CEntity eagerC) {
			this.eagerC = eagerC;
		}

		public List<DEntity> getdList() {
			return dList;
		}

		public void setdList(List<DEntity> dList) {
			this.dList = dList;
		}
	}

	@Entity(name = "DEntity")
	@Table(name = "D")
	public static class DEntity {

		@Id
		private Integer id;

		private String label;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "C_ID")
		private CEntity c;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "E_ID")
		private EEntity e;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public CEntity getC() {
			return c;
		}

		public void setC(CEntity c) {
			this.c = c;
		}

		public EEntity getE() {
			return e;
		}

		public void setE(EEntity e) {
			this.e = e;
		}
	}

	@Entity(name = "EEntity")
	@Table(name = "E")
	public static class EEntity {

		@Id
		private Integer id;

		private String label;

		@OneToMany(
				fetch = FetchType.LAZY,
				mappedBy = "e",
				cascade = CascadeType.ALL,
				orphanRemoval = true
		)
		private List<DEntity> dList = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

	}

}
