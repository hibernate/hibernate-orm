/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.caching;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Jpa(
		annotatedClasses = {
				CachingAndBatchTest.MyEntity1.class,
				CachingAndBatchTest.MyEntity2.class
		}
)
@JiraKey( value = "HHH-16025")
public class CachingAndBatchTest {

	@Test
	public void testIt(EntityManagerFactoryScope scope) {
		MyEntity1 myEntity1 = scope.fromTransaction(
				entityManager -> {
					MyEntity2 entity2 = new MyEntity2();
					entityManager.persist( entity2 );

					MyEntity1 entity1 = new MyEntity1();
					entity1.setRef( entity2 );
					entityManager.persist( entity1 );
					long entity1Id = entity1.getId();
					assertEquals( entity1Id, entity1.getId() );
					assertNotNull( entity1.getRef() );
					return entity1;
				}
		);

		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<MyEntity1> criteria = builder.createQuery( MyEntity1.class );
					Root<MyEntity1> root = criteria.from( MyEntity1.class );
					criteria.select( root );
					MyEntity1 entity1 = entityManager.createQuery( criteria ).getResultList().get( 0 );
					assertEquals( myEntity1.getId(), entity1.getId() );
					assertNotNull( entity1.getRef() );
				}
		);

		scope.inTransaction(
				entityManager -> {
					MyEntity1 entity1 = entityManager.find( MyEntity1.class, myEntity1.getId() );
					assertEquals( myEntity1.getId(), entity1.getId() );
					assertNotNull( entity1.getRef() );
				}
		);
	}

	@Entity
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
	public static class MyEntity1 {
		@Id()
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_sequence")
		private Long id;

		@Version
		private Long version;

		@ManyToOne
		@JoinColumn(name = "ref", nullable = true)
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

		@PostLoad
		@PreUpdate
		@PostPersist
		public void test() {
			System.out.println( "ref = " + ref );
		}
	}

	@Entity
	@BatchSize(size = 100)
	public static class MyEntity2 {
		@Id()
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_sequence")
		private Long id;

		public MyEntity2() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}


}
