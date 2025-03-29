/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.annotations.FlushModeType;
import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Yoann Rodiere
 */
@JiraKey(value = "HHH-12795")
@Jpa(annotatedClasses = {
		NamedQueryFlushModeTest.TestEntity.class
})
public class NamedQueryFlushModeTest {

	@Test
	public void testNamedQueryWithFlushModeManual(EntityManagerFactoryScope scope) {
		String queryName = "NamedQueryFlushModeManual";
		scope.inEntityManager(
				entityManager -> {
					Session s = entityManager.unwrap( Session.class );
					Query<?> query = s.getNamedQuery( queryName );
					assertEquals( FlushMode.MANUAL, query.getHibernateFlushMode() );
					// JPA flush mode is an approximation
					assertEquals( jakarta.persistence.FlushModeType.COMMIT, query.getFlushMode() );
				}
		);
	}

	@Test
	public void testNamedQueryWithFlushModeCommit(EntityManagerFactoryScope scope) {
		String queryName = "NamedQueryFlushModeCommit";
		scope.inEntityManager(
				entityManager -> {
					Session s = entityManager.unwrap( Session.class );
					Query<?> query = s.getNamedQuery( queryName );
					assertEquals( FlushMode.COMMIT, query.getHibernateFlushMode() );
					assertEquals( jakarta.persistence.FlushModeType.COMMIT, query.getFlushMode() );
				}
		);
	}

	@Test
	public void testNamedQueryWithFlushModeAuto(EntityManagerFactoryScope scope) {
		String queryName = "NamedQueryFlushModeAuto";
		scope.inEntityManager(
				entityManager -> {
					Session s = entityManager.unwrap( Session.class );
					Query<?> query = s.getNamedQuery( queryName );
					assertEquals( FlushMode.AUTO, query.getHibernateFlushMode() );
					assertEquals( jakarta.persistence.FlushModeType.AUTO, query.getFlushMode() );
				}
		);
	}

	@Test
	public void testNamedQueryWithFlushModeAlways(EntityManagerFactoryScope scope) {
		String queryName = "NamedQueryFlushModeAlways";
		scope.inEntityManager(
				entityManager -> {
					Session s = entityManager.unwrap( Session.class );
					Query<?> query = s.getNamedQuery( queryName );
					assertEquals( FlushMode.ALWAYS, query.getHibernateFlushMode() );
					// JPA flush mode is an approximation
					assertEquals( jakarta.persistence.FlushModeType.AUTO, query.getFlushMode() );
				}
		);
	}

	@Test
	public void testNamedQueryWithFlushModePersistenceContext(EntityManagerFactoryScope scope) {
		String queryName = "NamedQueryFlushModePersistenceContext";
		scope.inEntityManager(
				entityManager -> {
					Session s = entityManager.unwrap( Session.class );
					Query<?> query;

					// A null Hibernate flush mode means we will use whatever mode is set on the session
					// JPA doesn't allow null flush modes, so we expect some approximation of the flush mode to be returned

					s.setHibernateFlushMode( FlushMode.MANUAL );
					query = s.getNamedQuery( queryName );
					assertEquals( FlushMode.MANUAL, query.getHibernateFlushMode() );
					assertEquals( jakarta.persistence.FlushModeType.COMMIT, query.getFlushMode() );

					s.setHibernateFlushMode( FlushMode.COMMIT );
					query = s.getNamedQuery( queryName );
					assertEquals( FlushMode.COMMIT, query.getHibernateFlushMode() );
					assertEquals( jakarta.persistence.FlushModeType.COMMIT, query.getFlushMode() );

					s.setHibernateFlushMode( FlushMode.AUTO );
					query = s.getNamedQuery( queryName );
					assertEquals( FlushMode.AUTO, query.getHibernateFlushMode() );
					assertEquals( jakarta.persistence.FlushModeType.AUTO, query.getFlushMode() );

					s.setHibernateFlushMode( FlushMode.ALWAYS );
					query = s.getNamedQuery( queryName );
					assertEquals( FlushMode.ALWAYS, query.getHibernateFlushMode() );
					assertEquals( jakarta.persistence.FlushModeType.AUTO, query.getFlushMode() );
				}
		);
	}

	@Test
	public void testNamedNativeQueryWithFlushModeManual(EntityManagerFactoryScope scope) {
		String queryName = "NamedNativeQueryFlushModeManual";
		scope.inEntityManager(
				entityManager -> {
					Session s = entityManager.unwrap( Session.class );
					NativeQuery<?> query = s.getNamedNativeQuery( queryName );
					assertEquals( FlushMode.MANUAL, query.getHibernateFlushMode() );
				}
		);
	}

	@Test
	public void testNamedNativeQueryWithFlushModeCommit(EntityManagerFactoryScope scope) {
		String queryName = "NamedNativeQueryFlushModeCommit";
		scope.inEntityManager(
				entityManager -> {
					Session s = entityManager.unwrap( Session.class );
					NativeQuery<?> query = s.getNamedNativeQuery( queryName );
					assertEquals( FlushMode.COMMIT, query.getHibernateFlushMode() );
				}
		);
	}

	@Test
	public void testNamedNativeQueryWithFlushModeAuto(EntityManagerFactoryScope scope) {
		String queryName = "NamedNativeQueryFlushModeAuto";
		scope.inEntityManager(
				entityManager -> {
					Session s = entityManager.unwrap( Session.class );
					NativeQuery<?> query = s.getNamedNativeQuery( queryName );
					assertEquals( FlushMode.AUTO, query.getHibernateFlushMode() );
				}
		);
	}

	@Test
	public void testNamedNativeQueryWithFlushModeAlways(EntityManagerFactoryScope scope) {
		String queryName = "NamedNativeQueryFlushModeAlways";
		scope.inEntityManager(
				entityManager -> {
					Session s = entityManager.unwrap( Session.class );
					NativeQuery<?> query = s.getNamedNativeQuery( queryName );
					assertEquals( FlushMode.ALWAYS, query.getHibernateFlushMode() );
				}
		);
	}

	@Test
	public void testNamedNativeQueryWithFlushModePersistenceContext(EntityManagerFactoryScope scope) {
		String queryName = "NamedNativeQueryFlushModePersistenceContext";
		scope.inEntityManager(
				entityManager -> {
					Session s = entityManager.unwrap( Session.class );
					NativeQuery<?> query;

					// A null Hibernate flush mode means we will use whatever mode is set on the session
					// JPA doesn't allow null flush modes, so we expect some approximation of the flush mode to be returned

					s.setHibernateFlushMode( FlushMode.MANUAL );
					query = s.getNamedNativeQuery( queryName );
					assertEquals( FlushMode.MANUAL, query.getHibernateFlushMode() );
					assertEquals( jakarta.persistence.FlushModeType.COMMIT, query.getFlushMode() );

					s.setHibernateFlushMode( FlushMode.COMMIT );
					query = s.getNamedNativeQuery( queryName );
					assertEquals( FlushMode.COMMIT, query.getHibernateFlushMode() );
					assertEquals( jakarta.persistence.FlushModeType.COMMIT, query.getFlushMode() );

					s.setHibernateFlushMode( FlushMode.AUTO );
					query = s.getNamedNativeQuery( queryName );
					assertEquals( FlushMode.AUTO, query.getHibernateFlushMode() );
					assertEquals( jakarta.persistence.FlushModeType.AUTO, query.getFlushMode() );

					s.setHibernateFlushMode( FlushMode.ALWAYS );
					query = s.getNamedNativeQuery( queryName );
					assertEquals( FlushMode.ALWAYS, query.getHibernateFlushMode() );
					assertEquals( jakarta.persistence.FlushModeType.AUTO, query.getFlushMode() );
				}
		);
	}

	@Entity(name = "TestEntity")
	@NamedQuery(
			name = "NamedQueryFlushModeManual",
			query = "select e from TestEntity e where e.text = :text",
			flushMode = FlushModeType.MANUAL
	)
	@NamedQuery(
			name = "NamedQueryFlushModeCommit",
			query = "select e from TestEntity e where e.text = :text",
			flushMode = FlushModeType.COMMIT
	)
	@NamedQuery(
			name = "NamedQueryFlushModeAuto",
			query = "select e from TestEntity e where e.text = :text",
			flushMode = FlushModeType.AUTO
	)
	@NamedQuery(
			name = "NamedQueryFlushModeAlways",
			query = "select e from TestEntity e where e.text = :text",
			flushMode = FlushModeType.ALWAYS
	)
	@NamedQuery(
			name = "NamedQueryFlushModePersistenceContext",
			query = "select e from TestEntity e where e.text = :text",
			flushMode = FlushModeType.PERSISTENCE_CONTEXT
	)
	@NamedNativeQuery(
			name = "NamedNativeQueryFlushModeManual",
			query = "select * from TestEntity e where e.text = :text",
			resultClass = TestEntity.class,
			flushMode = FlushModeType.MANUAL
	)
	@NamedNativeQuery(
			name = "NamedNativeQueryFlushModeCommit",
			query = "select * from TestEntity e where e.text = :text",
			resultClass = TestEntity.class,
			flushMode = FlushModeType.COMMIT
	)
	@NamedNativeQuery(
			name = "NamedNativeQueryFlushModeAuto",
			query = "select * from TestEntity e where e.text = :text",
			resultClass = TestEntity.class,
			flushMode = FlushModeType.AUTO
	)
	@NamedNativeQuery(
			name = "NamedNativeQueryFlushModeAlways",
			query = "select * from TestEntity e where e.text = :text",
			resultClass = TestEntity.class,
			flushMode = FlushModeType.ALWAYS
	)
	@NamedNativeQuery(
			name = "NamedNativeQueryFlushModePersistenceContext",
			query = "select * from TestEntity e where e.text = :text",
			resultClass = TestEntity.class,
			flushMode = FlushModeType.PERSISTENCE_CONTEXT
	)

	public static class TestEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String text;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}
}
