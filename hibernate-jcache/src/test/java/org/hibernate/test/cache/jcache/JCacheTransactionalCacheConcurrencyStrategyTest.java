/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cache.jcache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

public class JCacheTransactionalCacheConcurrencyStrategyTest
		extends BaseNonConfigCoreFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		sqlStatementInterceptor = new SQLStatementInterceptor( sfb );
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );

		TestingJtaBootstrap.prepare( settings );
		settings.put( Environment.TRANSACTION_COORDINATOR_STRATEGY, "jta" );
		settings.put( Environment.CACHE_REGION_FACTORY, "jcache" );
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Parent.class,
				Child.class
		};
	}

	@Entity(name = "Parent")
	@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
	public static class Parent {

		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "parent")
		@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
		private List<Child> children = new ArrayList<Child>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void setChildren(List<Child> children) {
			this.children = children;
		}

		Child addChild() {
			final Child c = new Child();
			c.setParent( this );
			this.children.add( c );
			return c;
		}

	}

	@Entity(name = "Child")
	@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
	public static class Child {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(
				fetch = FetchType.LAZY
		)
		private Parent parent;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}


	@Test
	public void testTransactional() {
		Parent parent = new Parent();

		doInHibernate( this::sessionFactory, session -> {
			for ( int i = 0; i < 2; i++ ) {
				parent.addChild();

				session.persist( parent );
			}
		} );

		doInHibernate( this::sessionFactory, session -> {
			sqlStatementInterceptor.getSqlQueries().clear();

			Parent _parent = session.find( Parent.class, parent.getId() );

			assertEquals( 0, sqlStatementInterceptor.getSqlQueries().size() );

			assertEquals( 2, _parent.getChildren().size() );
		} );

		doInHibernate( this::sessionFactory, session -> {
			sqlStatementInterceptor.getSqlQueries().clear();

			Parent _parent = session.find( Parent.class, parent.getId() );

			assertEquals( 2, _parent.getChildren().size() );

			assertEquals( 0, sqlStatementInterceptor.getSqlQueries().size() );
		} );
	}

}
