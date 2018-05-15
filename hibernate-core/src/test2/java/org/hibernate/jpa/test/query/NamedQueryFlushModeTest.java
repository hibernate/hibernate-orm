/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.query;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.FlushMode;
import org.hibernate.annotations.FlushModeType;
import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Yoann Rodiere
 */
@TestForIssue(jiraKey = "HHH-12795")
public class NamedQueryFlushModeTest extends BaseCoreFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { TestEntity.class };
	}

	@Test
	public void testNamedQueryWithFlushModeManual() {
		String queryName = "NamedQueryFlushModeManual";
		TransactionUtil.doInHibernate( this::sessionFactory, s -> {
			Query<?> query = s.getNamedQuery( queryName );
			Assert.assertEquals( FlushMode.MANUAL, query.getHibernateFlushMode() );
			// JPA flush mode is an approximation
			Assert.assertEquals( javax.persistence.FlushModeType.COMMIT, query.getFlushMode() );
		} );
	}

	@Test
	public void testNamedQueryWithFlushModeCommit() {
		String queryName = "NamedQueryFlushModeCommit";
		TransactionUtil.doInHibernate( this::sessionFactory, s -> {
			Query<?> query = s.getNamedQuery( queryName );
			Assert.assertEquals( FlushMode.COMMIT, query.getHibernateFlushMode() );
			Assert.assertEquals( javax.persistence.FlushModeType.COMMIT, query.getFlushMode() );
		} );
	}

	@Test
	public void testNamedQueryWithFlushModeAuto() {
		String queryName = "NamedQueryFlushModeAuto";
		TransactionUtil.doInHibernate( this::sessionFactory, s -> {
			Query<?> query = s.getNamedQuery( queryName );
			Assert.assertEquals( FlushMode.AUTO, query.getHibernateFlushMode() );
			Assert.assertEquals( javax.persistence.FlushModeType.AUTO, query.getFlushMode() );
		} );
	}

	@Test
	public void testNamedQueryWithFlushModeAlways() {
		String queryName = "NamedQueryFlushModeAlways";
		TransactionUtil.doInHibernate( this::sessionFactory, s -> {
			Query<?> query = s.getNamedQuery( queryName );
			Assert.assertEquals( FlushMode.ALWAYS, query.getHibernateFlushMode() );
			// JPA flush mode is an approximation
			Assert.assertEquals( javax.persistence.FlushModeType.AUTO, query.getFlushMode() );
		} );
	}

	@Test
	public void testNamedQueryWithFlushModePersistenceContext() {
		String queryName = "NamedQueryFlushModePersistenceContext";
		TransactionUtil.doInHibernate( this::sessionFactory, s -> {
			Query<?> query;

			// A null Hibernate flush mode means we will use whatever mode is set on the session
			// JPA doesn't allow null flush modes, so we expect some approximation of the flush mode to be returned

			s.setHibernateFlushMode( FlushMode.MANUAL );
			query = s.getNamedQuery( queryName );
			Assert.assertNull( query.getHibernateFlushMode() );
			Assert.assertEquals( javax.persistence.FlushModeType.COMMIT, query.getFlushMode() );

			s.setHibernateFlushMode( FlushMode.COMMIT );
			query = s.getNamedQuery( queryName );
			Assert.assertNull( query.getHibernateFlushMode() );
			Assert.assertEquals( javax.persistence.FlushModeType.COMMIT, query.getFlushMode() );

			s.setHibernateFlushMode( FlushMode.AUTO );
			query = s.getNamedQuery( queryName );
			Assert.assertNull( query.getHibernateFlushMode() );
			Assert.assertEquals( javax.persistence.FlushModeType.AUTO, query.getFlushMode() );

			s.setHibernateFlushMode( FlushMode.ALWAYS );
			query = s.getNamedQuery( queryName );
			Assert.assertNull( query.getHibernateFlushMode() );
			Assert.assertEquals( javax.persistence.FlushModeType.AUTO, query.getFlushMode() );
		} );
	}

	@Test
	public void testNamedNativeQueryWithFlushModeManual() {
		String queryName = "NamedNativeQueryFlushModeManual";
		TransactionUtil.doInHibernate( this::sessionFactory, s -> {
			NativeQuery<?> query = s.getNamedNativeQuery( queryName );
			Assert.assertEquals( FlushMode.MANUAL, query.getHibernateFlushMode() );
		} );
	}

	@Test
	public void testNamedNativeQueryWithFlushModeCommit() {
		String queryName = "NamedNativeQueryFlushModeCommit";
		TransactionUtil.doInHibernate( this::sessionFactory, s -> {
			NativeQuery<?> query = s.getNamedNativeQuery( queryName );
			Assert.assertEquals( FlushMode.COMMIT, query.getHibernateFlushMode() );
		} );
	}

	@Test
	public void testNamedNativeQueryWithFlushModeAuto() {
		String queryName = "NamedNativeQueryFlushModeAuto";
		TransactionUtil.doInHibernate( this::sessionFactory, s -> {
			NativeQuery<?> query = s.getNamedNativeQuery( queryName );
			Assert.assertEquals( FlushMode.AUTO, query.getHibernateFlushMode() );
		} );
	}

	@Test
	public void testNamedNativeQueryWithFlushModeAlways() {
		String queryName = "NamedNativeQueryFlushModeAlways";
		TransactionUtil.doInHibernate( this::sessionFactory, s -> {
			NativeQuery<?> query = s.getNamedNativeQuery( queryName );
			Assert.assertEquals( FlushMode.ALWAYS, query.getHibernateFlushMode() );
		} );
	}

	@Test
	public void testNamedNativeQueryWithFlushModePersistenceContext() {
		String queryName = "NamedNativeQueryFlushModePersistenceContext";
		TransactionUtil.doInHibernate( this::sessionFactory, s -> {
			NativeQuery<?> query;

			// A null Hibernate flush mode means we will use whatever mode is set on the session
			// JPA doesn't allow null flush modes, so we expect some approximation of the flush mode to be returned

			s.setHibernateFlushMode( FlushMode.MANUAL );
			query = s.getNamedNativeQuery( queryName );
			Assert.assertNull( query.getHibernateFlushMode() );
			Assert.assertEquals( javax.persistence.FlushModeType.COMMIT, query.getFlushMode() );

			s.setHibernateFlushMode( FlushMode.COMMIT );
			query = s.getNamedNativeQuery( queryName );
			Assert.assertNull( query.getHibernateFlushMode() );
			Assert.assertEquals( javax.persistence.FlushModeType.COMMIT, query.getFlushMode() );

			s.setHibernateFlushMode( FlushMode.AUTO );
			query = s.getNamedNativeQuery( queryName );
			Assert.assertNull( query.getHibernateFlushMode() );
			Assert.assertEquals( javax.persistence.FlushModeType.AUTO, query.getFlushMode() );

			s.setHibernateFlushMode( FlushMode.ALWAYS );
			query = s.getNamedNativeQuery( queryName );
			Assert.assertNull( query.getHibernateFlushMode() );
			Assert.assertEquals( javax.persistence.FlushModeType.AUTO, query.getFlushMode() );
		} );
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
