/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

import org.hibernate.graph.GraphSemantic;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hibernate.testing.hamcrest.InitializationCheckMatcher.isInitialized;
import static org.hibernate.testing.hamcrest.InitializationCheckMatcher.isNotInitialized;

/**
 * @author Andrea Boriero
 * @author Nathan Xu
 */
@DomainModel(
		annotatedClasses = {
				LoadAndFetchGraphTest.AEntity.class,
				LoadAndFetchGraphTest.BEntity.class,
				LoadAndFetchGraphTest.CEntity.class,
				LoadAndFetchGraphTest.DEntity.class,
				LoadAndFetchGraphTest.EEntity.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
@JiraKey(value = "HHH-14097")
public class LoadAndFetchGraphTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityManager entityManager = session.unwrap( EntityManager.class );
					AEntity a1 = new AEntity();
					a1.setId( 1 );
					a1.setLabel( "A1" );

					AEntity a2 = new AEntity();
					a2.setId( 2 );
					a2.setLabel( "A2" );

					entityManager.persist( a1 );
					entityManager.persist( a2 );

					BEntity b1 = new BEntity();
					b1.setId( 1 );
					b1.setLabel( "B1" );

					BEntity b2 = new BEntity();
					b2.setId( 2 );
					b2.setLabel( "B2" );

					entityManager.persist( b1 );
					entityManager.persist( b2 );

					EEntity e1 = new EEntity();
					e1.setId( 1 );
					e1.setLabel( "E1" );

					EEntity e2 = new EEntity();
					e2.setId( 2 );
					e2.setLabel( "E2" );

					EEntity e3 = new EEntity();
					e3.setId( 3 );
					e3.setLabel( "E3" );

					EEntity e4 = new EEntity();
					e4.setId( 4 );
					e4.setLabel( "E4" );

					entityManager.persist( e1 );
					entityManager.persist( e2 );
					entityManager.persist( e3 );
					entityManager.persist( e4 );

					DEntity d1 = new DEntity();
					d1.setId( 1 );
					d1.setLabel( "D1" );
					d1.setE( e1 );

					DEntity d2 = new DEntity();
					d2.setId( 2 );
					d2.setLabel( "D2" );
					d2.setE( e2 );

					CEntity c1 = new CEntity();
					c1.setId( 1 );
					c1.setLabel( "C1" );
					c1.setA( a1 );
					c1.setB( b1 );
					c1.addD( d1 );
					c1.addD( d2 );

					entityManager.persist( c1 );

					DEntity d3 = new DEntity();
					d3.setId( 3 );
					d3.setLabel( "D3" );
					d3.setE( e3 );

					DEntity d4 = new DEntity();
					d4.setId( 4 );
					d4.setLabel( "D4" );
					d4.setE( e4 );

					CEntity c2 = new CEntity();
					c2.setId( 2 );
					c2.setLabel( "C2" );
					c2.setA( a2 );
					c2.setB( b2 );
					c2.addD( d3 );
					c2.addD( d4 );

					entityManager.persist( c2 );

					CEntity c3 = new CEntity();
					c3.setId( 3 );
					c3.setLabel( "C3" );

					entityManager.persist( c3 );

					c1.setC( c2 );
					c2.setC( c3 );

					int id = 5;
					for ( int i = 0; i < 10; i++ ) {
						DEntity dn = new DEntity();
						dn.setId( id++ );
						dn.setLabel( "label" );
						dn.setE( e3 );
						entityManager.persist( dn );
					}
				}
		);
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testQueryById(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					EntityManager entityManager = session.unwrap( EntityManager.class );
					TypedQuery<CEntity> query = entityManager.createQuery(
							"select c from CEntity as c where c.id = :cid ",
							CEntity.class
					);
					query.setParameter( "cid", 1 );
					CEntity cEntity = query.getSingleResult();

					assertThat( cEntity.getA(), isNotInitialized() );
					assertThat( cEntity.getB(), isNotInitialized() );
					assertThat( cEntity.getC(), isNotInitialized() );
					assertThat( cEntity.getDList(), isNotInitialized() );

					assertThat( statementInspector.getSqlQueries(), hasSize( 1 ) );
				}
		);
	}

	@Test
	void testQueryByIdWithLoadGraph(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					EntityManager entityManager = session.unwrap( EntityManager.class );
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

					assertThat( cEntity.getA(), isInitialized() );
					assertThat( cEntity.getB(), isInitialized() );
					assertThat( cEntity.getC(), isNotInitialized() );
					assertThat( cEntity.getDList(), isInitialized() );
					cEntity.getDList().forEach( dEntity -> assertThat( dEntity.getE(), isInitialized() ) );

					assertThat( statementInspector.getSqlQueries(), hasSize( 1 ) );
				}
		);
	}

	@Test
	void testQueryByIdWithFetchGraph(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					EntityManager entityManager = session.unwrap( EntityManager.class );
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
					assertThat( cEntity.getA(), isInitialized() );
					assertThat( cEntity.getB(), isInitialized() );
					assertThat( cEntity.getC(), isNotInitialized() );
					assertThat( cEntity.getDList(), isInitialized() );
					cEntity.getDList().forEach( dEntity -> assertThat( dEntity.getE(), isInitialized() ) );

					assertThat( statementInspector.getSqlQueries(), hasSize( 1 ) );
				}
		);
	}

	@Test
	void testQueryByIdWithFetchGraph2(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					EntityManager entityManager = session.unwrap( EntityManager.class );
					EntityGraph<CEntity> entityGraph = entityManager.createEntityGraph( CEntity.class );
					entityGraph.addSubgraph( "c" ).addAttributeNodes( "a" );

					TypedQuery<CEntity> query = entityManager.createQuery(
							"select c from CEntity as c where c.id = :cid ",
							CEntity.class
					);
					query.setHint( GraphSemantic.FETCH.getJpaHintName(), entityGraph );
					query.setParameter( "cid", 1 );

					CEntity cEntity = query.getSingleResult();
					assertThat( cEntity.getC(), isInitialized() );
					assertThat( cEntity.getC().getA(), isInitialized() );
					assertThat( cEntity.getC().getC(), isNotInitialized() );

					assertThat( statementInspector.getSqlQueries(), hasSize( 1 ) );
				}
		);
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

		public List<CEntity> getCList() {
			return cList;
		}

		public void setCList(List<CEntity> cList) {
			this.cList = cList;
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

		public List<CEntity> getCList() {
			return cList;
		}

		public void setCList(List<CEntity> cList) {
			this.cList = cList;
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

		public List<DEntity> getDList() {
			return dList;
		}

		public void setDList(List<DEntity> dList) {
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

		public List<DEntity> getDList() {
			return dList;
		}

		public void setDList(List<DEntity> dList) {
			this.dList = dList;
		}
	}

}
