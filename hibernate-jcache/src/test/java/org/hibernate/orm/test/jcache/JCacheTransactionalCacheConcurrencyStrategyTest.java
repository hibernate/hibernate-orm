/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jcache;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.CacheSettings.CACHE_REGION_FACTORY;
import static org.hibernate.cfg.TransactionSettings.ENABLE_LAZY_LOAD_NO_TRANS;
import static org.hibernate.cfg.TransactionSettings.TRANSACTION_COORDINATOR_STRATEGY;

@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(
		settings = {
				@Setting( name = CACHE_REGION_FACTORY, value = "jcache"),
				@Setting( name = TRANSACTION_COORDINATOR_STRATEGY, value = "jta"),
				@Setting( name = ENABLE_LAZY_LOAD_NO_TRANS, value = "true"),
		},
		settingConfigurations = @SettingConfiguration(configurer = TestingJtaBootstrap.class)
)
@DomainModel(annotatedClasses = {
		JCacheTransactionalCacheConcurrencyStrategyTest.Parent.class,
		JCacheTransactionalCacheConcurrencyStrategyTest.Child.class
})
@SessionFactory(useCollectingStatementInspector = true)
public class JCacheTransactionalCacheConcurrencyStrategyTest {

	@Test
	public void testTransactional(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			Parent parent = new Parent( 1, "first" );
			for ( int i = 0; i < 2; i++ ) {
				final Child child = new Child( i, "child #" + i, parent );
				parent.addChild( child );
			}
			session.persist( parent );
		} );

		factoryScope.inTransaction(  (session) -> {
			sqlCollector.clear();

			Parent parent = session.find( Parent.class, 1 );
			assertThat( sqlCollector.getSqlQueries() ).isEmpty();
			assertThat( parent.getChildren() ).hasSize( 2 );
		} );
	}

	@Entity(name = "Parent")
	@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
	public static class Parent {
		@Id
		private Integer id;
		private String name;

		@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "parent")
		@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
		private List<Child> children = new ArrayList<>();

		public Parent() {
		}

		public Parent(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
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

		void addChild(Child c) {
			children.add( c );
		}
	}

	@Entity(name = "Child")
	@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
	public static class Child {
		@Id
		private Integer id;
		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		private Parent parent;

		public Child() {
		}

		public Child(Integer id, String name, Parent parent) {
			this.id = id;
			this.name = name;
			this.parent = parent;
			parent.addChild(this);
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}


}
