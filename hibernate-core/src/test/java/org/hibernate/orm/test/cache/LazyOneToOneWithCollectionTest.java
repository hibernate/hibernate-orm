/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		LazyOneToOneWithCollectionTest.Contract.class,
		LazyOneToOneWithCollectionTest.Diagram.class,
		LazyOneToOneWithCollectionTest.Entry.class,
} )
@SessionFactory( generateStatistics = true )
@ServiceRegistry( settings = @Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ) )
public class LazyOneToOneWithCollectionTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Diagram diagram = new Diagram( "diagram" );
			diagram.getEntries().add( new Entry( "entry_1", diagram ) );
			diagram.getEntries().add( new Entry( "entry_2", diagram ) );
			final Contract contract = new Contract( 1L, "contract", diagram );
			diagram.setContract( contract );
			session.persist( diagram );
			session.persist( contract );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "from Diagram", Diagram.class ).getResultList().forEach( d -> d.setContract( null ) );
			session.flush();
			session.createMutationQuery( "delete from Entry" ).executeUpdate();
			session.createMutationQuery( "delete from Contract" ).executeUpdate();
			session.createMutationQuery( "delete from Diagram" ).executeUpdate();
		} );
	}

	@Test
	public void testWithCachedContract(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evictAllRegions();
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction( session -> {
			final Contract contract = session.find( Contract.class, 1L );
			assertThat( Hibernate.isInitialized( contract.getDiagram() ) ).isFalse();
		} );

		assertThat( statistics.getSecondLevelCachePutCount() ).isEqualTo( 1 );

		scope.inTransaction( session -> {
			final Diagram diagram = session.createQuery(
					"select d from Diagram d join fetch d.entries",
					Diagram.class
			).getSingleResult();
			assertDiagramResult( diagram );
		} );

		assertThat( statistics.getSecondLevelCachePutCount() ).isEqualTo( 2 );
		assertThat( statistics.getSecondLevelCacheHitCount() ).isEqualTo( 1 );
	}

	@Test
	public void testWithoutCachedContract(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evictAllRegions();
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction( session -> {
			final Diagram diagram = session.createQuery(
					"select d from Diagram d join fetch d.entries",
					Diagram.class
			).getSingleResult();
			assertDiagramResult( diagram );
		} );

		assertThat( statistics.getSecondLevelCachePutCount() ).isEqualTo( 2 );
		assertThat( statistics.getSecondLevelCacheHitCount() ).isEqualTo( 0 );
	}

	private void assertDiagramResult(Diagram diagram) {
		// returned object should be an entity instance, not a proxy
		assertThat( HibernateProxy.extractLazyInitializer( diagram ) ).isNull();
		assertThat( diagram.getContract() ).isNotNull();
		assertThat( diagram.getContract().getDiagram() ).isSameAs( diagram );
		assertThat( Hibernate.isInitialized( diagram.getEntries() ) ).isTrue();
		assertThat( diagram.getEntries() ).hasSize( 2 );
	}

	@Entity( name = "Contract" )
	@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
	public static class Contract {
		@Id
		private Long id;

		private String name;

		@OneToOne( fetch = FetchType.LAZY )
		private Diagram diagram;

		public Contract() {
		}

		public Contract(Long id, String name, Diagram diagram) {
			this.id = id;
			this.name = name;
			this.diagram = diagram;
		}

		public Diagram getDiagram() {
			return diagram;
		}
	}

	@Entity( name = "Diagram" )
	@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
	public static class Diagram {
		@Id
		@GeneratedValue
		private Long id;

		@Column
		private String name;

		@OneToOne
		private Contract contract;

		@OneToMany( mappedBy = "diagram", cascade = CascadeType.ALL )
		private Set<Entry> entries = new HashSet<>();

		public Diagram() {
		}

		public Diagram(String name) {
			this.name = name;
		}

		public Contract getContract() {
			return contract;
		}

		public void setContract(Contract contract) {
			this.contract = contract;
		}

		public Set<Entry> getEntries() {
			return entries;
		}
	}

	@Entity( name = "Entry" )
	public static class Entry {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne
		private Diagram diagram;

		public Entry() {
		}

		public Entry(String name, Diagram diagram) {
			this.name = name;
			this.diagram = diagram;
		}
	}
}
