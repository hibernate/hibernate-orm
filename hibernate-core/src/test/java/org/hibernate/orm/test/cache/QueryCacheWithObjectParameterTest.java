/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import java.util.List;
import java.util.Set;

import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.Query;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		QueryCacheWithObjectParameterTest.Parent.class,
		QueryCacheWithObjectParameterTest.Child.class
})
@SessionFactory(generateStatistics = true)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true")
		}
)
public class QueryCacheWithObjectParameterTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent p = new Parent( "John" );
					Address address = new Address( "via Milano", "Roma" );
					p.setAddress( address );
					session.persist( p );

					Child c = new Child( "Alex", p );
					session.persist( c );
				}
		);
	}

	@Test
	public void testQueryWithEmbeddableParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					evictQueryRegion( session );
					Query<Parent> queryParent = session.createQuery(
							"from Parent p where p.address = :address",
							Parent.class
					);
					queryParent.setParameter( "address", new Address( "via Milano", "Roma" ) );
					queryParent.setCacheable( true );

					List<Parent> resultList = queryParent.getResultList();
					assertThat( resultList ).hasSize( 1 );

					CacheRegionStatistics defaultQueryCacheRegionStatistics = getQueryCacheRegionStatistics( session );
					assertThat( defaultQueryCacheRegionStatistics.getHitCount() ).isEqualTo( 0 );
				}
		);

		scope.inTransaction(
				session -> {
					Query<Parent> queryParent = session.createQuery(
							"from Parent p where p.address = :address",
							Parent.class
					);
					queryParent.setParameter( "address", new Address( "via Milano", "Roma" ) );
					queryParent.setCacheable( true );

					List<Parent> resultList = queryParent.getResultList();
					assertThat( resultList ).hasSize( 1 );

					CacheRegionStatistics defaultQueryCacheRegionStatistics = getQueryCacheRegionStatistics( session );
					assertThat( defaultQueryCacheRegionStatistics.getHitCount() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testQueryWithEmbeddableParameterWithANull(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					evictQueryRegion( session );
					Query<Parent> queryParent = session.createQuery(
							"from Parent p where p.address = :address",
							Parent.class
					);
					queryParent.setParameter( "address", new Address( "via Milano", null ) );
					queryParent.setCacheable( true );

					List<Parent> resultList = queryParent.getResultList();
					assertThat( resultList ).hasSize( 0 );

					CacheRegionStatistics defaultQueryCacheRegionStatistics = getQueryCacheRegionStatistics( session );
					assertThat( defaultQueryCacheRegionStatistics.getHitCount() ).isEqualTo( 0 );
				}
		);

		scope.inTransaction(
				session -> {
					Query<Parent> queryParent = session.createQuery(
							"from Parent p where p.address = :address",
							Parent.class
					);
					queryParent.setParameter( "address", new Address( "via Milano", null ) );
					queryParent.setCacheable( true );

					List<Parent> resultList = queryParent.getResultList();
					assertThat( resultList ).hasSize( 0 );

					CacheRegionStatistics defaultQueryCacheRegionStatistics = getQueryCacheRegionStatistics( session );
					assertThat( defaultQueryCacheRegionStatistics.getHitCount() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testQueryCacheHits(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					evictQueryRegion( session );
					Query<Parent> queryParent = session.createQuery(
							"from Parent p where p.name = 'John'",
							Parent.class
					);
					List<Parent> p = queryParent.getResultList();
					assertThat( p ).hasSize( 1 );

					Query<Child> queryChildren = session.createQuery( "from Child c where c.parent = ?1", Child.class );
					queryChildren.setParameter( 1, p.get( 0 ) );
					queryChildren.setCacheable( true );
					List<Child> c = queryChildren.getResultList();
					assertThat( c ).hasSize( 1 );

					CacheRegionStatistics defaultQueryCacheRegionStatistics = getQueryCacheRegionStatistics( session );
					assertThat( defaultQueryCacheRegionStatistics.getHitCount() ).isEqualTo( 0 );
				}
		);

		scope.inTransaction(
				session -> {
					Query<Parent> queryParent = session.createQuery(
							"from Parent p where p.name = 'John'",
							Parent.class
					);
					List<Parent> p = queryParent.getResultList();
					assertThat( p ).hasSize( 1 );

					Query<Child> queryChildren = session.createQuery( "from Child c where c.parent = ?1", Child.class );
					queryChildren.setParameter( 1, p.get( 0 ) );
					queryChildren.setCacheable( true );
					List<Child> c = queryChildren.getResultList();
					assertThat( c ).hasSize( 1 );

					CacheRegionStatistics defaultQueryCacheRegionStatistics = getQueryCacheRegionStatistics( session );
					assertThat( defaultQueryCacheRegionStatistics.getHitCount() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testQueryCacheHits2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					evictQueryRegion( session );
					Query<Parent> queryParent = session.createQuery(
							"from Parent p where p.name = 'John'",
							Parent.class
					);
					List<Parent> p = queryParent.getResultList();
					assertThat( p ).hasSize( 1 );

					Query<Child> queryChildren = session.createQuery(
							"from Child c where c.parent.id = ?1",
							Child.class
					);
					queryChildren.setParameter( 1, p.get( 0 ).getId() );
					queryChildren.setCacheable( true );
					List<Child> c = queryChildren.getResultList();
					assertThat( c ).hasSize( 1 );

					CacheRegionStatistics defaultQueryCacheRegionStatistics = getQueryCacheRegionStatistics( session );
					assertThat( defaultQueryCacheRegionStatistics.getHitCount() ).isEqualTo( 0 );
				}
		);

		scope.inTransaction(
				session -> {
					Query<Parent> queryParent = session.createQuery(
							"from Parent p where p.name = 'John'",
							Parent.class
					);
					List<Parent> p = queryParent.getResultList();
					assertThat( p ).hasSize( 1 );

					Query<Child> queryChildren = session.createQuery(
							"from Child c where c.parent.id = ?1",
							Child.class
					);
					queryChildren.setParameter( 1, p.get( 0 ).getId() );
					queryChildren.setCacheable( true );
					List<Child> c = queryChildren.getResultList();
					assertThat( c ).hasSize( 1 );

					CacheRegionStatistics defaultQueryCacheRegionStatistics = getQueryCacheRegionStatistics( session );
					assertThat( defaultQueryCacheRegionStatistics.getHitCount() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testQueryCacheHitsNullParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					evictQueryRegion( session );
					Query<Parent> queryParent = session.createQuery(
							"from Parent p where p.name = 'John'",
							Parent.class
					);
					List<Parent> p = queryParent.getResultList();
					assertThat( p ).hasSize( 1 );

					Query<Child> queryChildren = session.createQuery(
							"from Child c where c.parent.id = ?1",
							Child.class
					);
					queryChildren.setParameter( 1, null );
					queryChildren.setCacheable( true );
					List<Child> c = queryChildren.getResultList();
					assertThat( c ).hasSize( 0 );

					CacheRegionStatistics defaultQueryCacheRegionStatistics = getQueryCacheRegionStatistics( session );
					assertThat( defaultQueryCacheRegionStatistics.getHitCount() ).isEqualTo( 0 );
				}
		);

		scope.inTransaction(
				session -> {
					Query<Parent> queryParent = session.createQuery(
							"from Parent p where p.name = 'John'",
							Parent.class
					);
					List<Parent> p = queryParent.getResultList();
					assertThat( p ).hasSize( 1 );

					Query<Child> queryChildren = session.createQuery(
							"from Child c where c.parent.id = ?1",
							Child.class
					);
					queryChildren.setParameter( 1, null );
					queryChildren.setCacheable( true );
					List<Child> c = queryChildren.getResultList();
					assertThat( c ).hasSize( 0 );

					CacheRegionStatistics defaultQueryCacheRegionStatistics = getQueryCacheRegionStatistics( session );
					assertThat( defaultQueryCacheRegionStatistics.getHitCount() ).isEqualTo( 1 );
				}
		);
	}

	private static void evictQueryRegion(SessionImplementor session) {
		session.getSessionFactory()
				.getCache()
				.evictQueryRegion( RegionFactory.DEFAULT_QUERY_RESULTS_REGION_UNQUALIFIED_NAME );
		session.getSessionFactory().getStatistics().clear();
	}

	private static CacheRegionStatistics getQueryCacheRegionStatistics(SessionImplementor session) {
		StatisticsImplementor statistics = session.getSessionFactory().getStatistics();
		return statistics.getQueryRegionStatistics( RegionFactory.DEFAULT_QUERY_RESULTS_REGION_UNQUALIFIED_NAME );
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		private Address address;

		@OneToMany(cascade = { CascadeType.PERSIST }, fetch = FetchType.LAZY)
		private Set<Child> children;

		public Parent() {
		}

		public Parent(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<Child> getChildren() {
			return children;
		}

		public void setChildren(Set<Child> children) {
			this.children = children;
		}

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST }, fetch = FetchType.LAZY)
		private Parent parent;

		public Child() {
		}

		public Child(String name, Parent parent) {
			this.name = name;
			this.parent = parent;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}

	@Embeddable
	public static class Address {
		private String street;
		private String city;

		public Address() {
		}

		public Address(String street, String city) {
			this.street = street;
			this.city = city;
		}

		public String getStreet() {
			return street;
		}

		public String getCity() {
			return city;
		}
	}

}
