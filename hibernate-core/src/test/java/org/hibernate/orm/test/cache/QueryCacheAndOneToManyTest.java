/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import java.util.List;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernateHints;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Query;
import jakarta.persistence.Version;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				QueryCacheAndOneToManyTest.MyEntity1.class,
				QueryCacheAndOneToManyTest.MyEntity2.class
		}
		,
		properties = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true")
		}
)
@JiraKey("HHH-16471")
public class QueryCacheAndOneToManyTest {

	@AfterEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();

	}

	@Test
	public void testQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					MyEntity2 entity2 = new MyEntity2();
					entityManager.persist( entity2 );

					entityManager.flush();

					CriteriaBuilder builder = entityManager.getCriteriaBuilder();

					CriteriaQuery<MyEntity1> criteria1 = builder.createQuery( MyEntity1.class );
					Root<MyEntity1> root1 = criteria1.from( MyEntity1.class );
					Subquery<MyEntity2> subquery2 = criteria1.subquery( MyEntity2.class );
					Root<MyEntity2> subroot2 = subquery2.from( MyEntity2.class );
					subquery2.select( subroot2 ).where( builder.equal( subroot2, entity2 ) );
					criteria1.select( root1 ).where( root1.get( "ref" ).in( subquery2 ) );

					List<MyEntity1> entity1s = entityManager.createQuery( criteria1 )
							.setHint( HibernateHints.HINT_CACHEABLE, true )
							.getResultList();
					assertThat( entity1s.size() ).isEqualTo( 0 );

					CriteriaQuery<MyEntity2> criteria2 = builder.createQuery( MyEntity2.class );
					Root<MyEntity2> root2 = criteria2.from( MyEntity2.class );
					criteria2.select( root2 ).where( builder.equal( root2, entity2 ) );
					List<MyEntity2> entity2s = entityManager.createQuery( criteria2 )
							.setHint( HibernateHints.HINT_CACHEABLE, true )
							.getResultList();
					assertThat( entity2s.size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testQuery2(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					MyEntity2 entity2 = new MyEntity2();
					MyEntity1 entity1 = new MyEntity1();
					entity1.setRef( entity2 );
					entityManager.persist( entity2 );
					entityManager.persist( entity1 );

					entityManager.flush();

					CriteriaBuilder builder = entityManager.getCriteriaBuilder();

					CriteriaQuery<MyEntity1> criteria1 = builder.createQuery( MyEntity1.class );
					Root<MyEntity1> root1 = criteria1.from( MyEntity1.class );
					Subquery<MyEntity2> subquery2 = criteria1.subquery( MyEntity2.class );
					Root<MyEntity2> subroot2 = subquery2.from( MyEntity2.class );
					subquery2.select( subroot2 ).where( builder.equal( subroot2, entity2 ) );
					criteria1.select( root1 ).where( root1.get( "ref" ).in( subquery2 ) );

					List<MyEntity1> entities1s = entityManager.createQuery( criteria1 )
							.setHint( HibernateHints.HINT_CACHEABLE, true )
							.getResultList();

					assertThat( entities1s.size() ).isEqualTo( 1 );

					CriteriaQuery<MyEntity2> criteria2 = builder.createQuery( MyEntity2.class );
					Root<MyEntity2> root2 = criteria2.from( MyEntity2.class );
					criteria2.select( root2 ).where( builder.equal( root2, entity2 ) );

					List<MyEntity2> entity2s = entityManager.createQuery( criteria2 )
							.setHint( HibernateHints.HINT_CACHEABLE, true )
							.getResultList();
					assertThat( entity2s.size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testQuery3(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					MyEntity2 entity2 = new MyEntity2();
					MyEntity1 entity1 = new MyEntity1();
					entity1.setRef( entity2 );
					entityManager.persist( entity2 );
					entityManager.persist( entity1 );

					entityManager.flush();

					Query query = entityManager.createQuery( "select e1 from MyEntity1 e1 where e1.ref = :myEntity2" )
							.setParameter( "myEntity2", entity2 )
							.setHint( HibernateHints.HINT_CACHEABLE, true );
					List results = query.getResultList();
					assertThat( results.size() ).isEqualTo( 1 );
				}
		);
	}

	@Entity(name = "MyEntity1")
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
	public static class MyEntity1 {
		@Id
		@GeneratedValue
		private Long id;

		@Version
		private Long version;

		@ManyToOne
		@JoinColumn(name = "ref")
		private MyEntity2 ref;

		public MyEntity1() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}

		public MyEntity2 getRef() {
			return ref;
		}

		public void setRef(MyEntity2 ref) {
			this.ref = ref;
		}

	}

	@Entity(name = "MyEntity2")
	public static class MyEntity2 {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public MyEntity2() {
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
