/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.proxy.concrete;

import jakarta.persistence.Id;
import jakarta.persistence.Cacheable;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;


import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @author Guillaume Toison
 */
@JiraKey( "HHH-18872" )
@DomainModel(
		annotatedClasses = {
				ConcreteProxyCacheTest.TestNode.class, ConcreteProxyCacheTest.TestCompositeNode.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
public class ConcreteProxyCacheTest {

	@Test
	public void testManyToOne2LcLoadOfConcreteProxy(SessionFactoryScope scope) throws Exception {
		scope.inTransaction( s -> {
			TestCompositeNode node1 = new TestCompositeNode( 1 );
			TestCompositeNode node2 = new TestCompositeNode( 2 );
			node1.setParent( node2 );

			s.persist( node1 );
			s.persist( node2 );
		} );

		scope.inSession( s -> {
			// Test the case when node1 is retrieved from 2LC but node2 is not in cache
			// Hibernate would either need to hit the database to retrieve the type or to the store the type in node1's cache entry
			s.getSessionFactory().getCache().evict( TestCompositeNode.class, 2 );

			TestCompositeNode node1 = s.getReference( TestCompositeNode.class, 1 );
			assertTrue( node1.getParent() instanceof TestCompositeNode , "Expecting object to be an instance of TestCompositeNode but the class was " + node1.getParent().getClass() );
		} );

	}

	@Entity
	@Table(name = "node")
	@Cacheable
	@ConcreteProxy
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "type")
	@DiscriminatorValue(value = "SIMPLE")
	public static class TestNode {
		private Integer id;

		public TestNode() {
		}

		public TestNode(Integer id) {
			this.id = id;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity
	@DiscriminatorValue("COMPOSITE")
	public static class TestCompositeNode extends TestNode {
		private TestNode parent;

		public TestCompositeNode() {
		}

		public TestCompositeNode(Integer id) {
			super(id);
		}

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "parent_id")
		public TestNode getParent() {
			return parent;
		}

		public void setParent(TestNode parent) {
			this.parent = parent;
		}
	}
}
