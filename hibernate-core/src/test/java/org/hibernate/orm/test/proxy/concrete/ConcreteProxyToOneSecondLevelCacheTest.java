/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.proxy.concrete;

import jakarta.persistence.Cacheable;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		ConcreteProxyToOneSecondLevelCacheTest.TestNode.class,
		ConcreteProxyToOneSecondLevelCacheTest.TestCompositeNode.class
})
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.GENERATE_STATISTICS, value = "true"),
		@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true")
})
@SessionFactory
@BytecodeEnhanced(runNotEnhancedAsWell = true)
@Jira("https://hibernate.atlassian.net/browse/HHH-18872")
public class ConcreteProxyToOneSecondLevelCacheTest {
	@Test
	public void testToOneInCacheGetReference(SessionFactoryScope scope) {
		final Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();
		// load only the node with ID 1 into the 2LC
		scope.inTransaction( session -> {
			assertThat( session.find( TestCompositeNode.class, 1 ) ).isNotNull();
			assertThat( session.find( TestCompositeNode.class, 2 ) ).isNotNull();
		} );
		assertCacheStats( stats, 0, 2, 2 );
		scope.inSession( session -> {
			final TestCompositeNode node1 = session.getReference( TestCompositeNode.class, 1 );
			assertThat( Hibernate.isInitialized( node1 ) ).isFalse();
			// this triggers node1 initialization, but should maintain laziness for parent
			final TestNode parent = node1.getParent();
			assertThat( Hibernate.isInitialized( node1 ) ).isTrue();
			// node 1 will be loaded from cache
			assertCacheStats( stats, 1, 2, 2 );
			assertParent( parent, stats, 2 );
		} );
	}

	@Test
	public void testToOneNotInCacheGetReference(SessionFactoryScope scope) {
		final Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();
		// load only the node with ID 1 into the 2LC
		scope.inTransaction( session -> assertThat( session.find( TestCompositeNode.class, 1 ) ).isNotNull() );
		assertCacheStats( stats, 0, 1, 1 );
		scope.inSession( session -> {
			final TestCompositeNode node1 = session.getReference( TestCompositeNode.class, 1 );
			assertThat( Hibernate.isInitialized( node1 ) ).isFalse();
			// this triggers node1 initialization, but should maintain laziness for parent
			final TestNode parent = node1.getParent();
			assertThat( Hibernate.isInitialized( node1 ) ).isTrue();
			// node 1 will be loaded from cache
			assertCacheStats( stats, 1, 1, 1 );
			assertParent( parent, stats, 1 );
		} );
	}

	@Test
	public void testToOneInCacheFind(SessionFactoryScope scope) {
		final Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();
		// load only the node with ID 1 into the 2LC
		scope.inTransaction( session -> {
			assertThat( session.find( TestCompositeNode.class, 1 ) ).isNotNull();
			assertThat( session.find( TestCompositeNode.class, 2 ) ).isNotNull();
		} );
		assertCacheStats( stats, 0, 2, 2 );
		scope.inSession( session -> {
			final TestCompositeNode node1 = session.find( TestCompositeNode.class, 1 );
			assertThat( Hibernate.isInitialized( node1 ) ).isTrue();
			// node 1 will be loaded from cache
			assertCacheStats( stats, 1, 2, 2 );
			assertParent( node1.getParent(), stats, 2 );
		} );
	}

	@Test
	public void testToOneNotInCacheFind(SessionFactoryScope scope) {
		final Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();
		// load only the node with ID 1 into the 2LC
		scope.inTransaction( session -> assertThat( session.find( TestCompositeNode.class, 1 ) ).isNotNull() );
		assertCacheStats( stats, 0, 1, 1 );
		scope.inSession( session -> {
			final TestCompositeNode node1 = session.find( TestCompositeNode.class, 1 );
			assertThat( Hibernate.isInitialized( node1 ) ).isTrue();
			// node 1 will be loaded from cache
			assertCacheStats( stats, 1, 1, 1 );
			assertParent( node1.getParent(), stats, 1 );
		} );
	}

	private static void assertParent(final TestNode parent, final Statistics stats, final long hits) {
		assertThat( TestCompositeNode.class ).as( "Expecting parent proxy to be narrowed to concrete type" )
				.isAssignableFrom( parent.getClass() );
		final TestCompositeNode parentComposite = (TestCompositeNode) parent;
		assertThat( Hibernate.isInitialized( parentComposite ) ).isFalse();
		assertThat( parentComposite.getName() ).isEqualTo( "parent_node" );
		assertThat( Hibernate.isInitialized( parentComposite ) ).isTrue();
		// node 2 will not be found in cache
		assertCacheStats( stats, hits, 2, 2 );
		assertThat( parentComposite ).as( String.format(
				"Expecting parent to be an instance of TestCompositeNode but was: [%s]",
				parent.getClass()
		) ).isInstanceOf( TestCompositeNode.class );
	}

	private static void assertCacheStats(final Statistics stats, final long hits, final long misses, final long puts) {
		assertThat( stats.getSecondLevelCacheHitCount() ).isEqualTo( hits );
		assertThat( stats.getSecondLevelCacheMissCount() ).isEqualTo( misses );
		assertThat( stats.getSecondLevelCachePutCount() ).isEqualTo( puts );
	}

	@BeforeEach
	public void clearCache(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestCompositeNode node1 = new TestCompositeNode( 1, "child_node" );
			final TestCompositeNode node2 = new TestCompositeNode( 2, "parent_node" );
			node1.setParent( node2 );
			session.persist( node1 );
			session.persist( node2 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "TestNode")
	@Cacheable
	@ConcreteProxy
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "disc_col")
	@DiscriminatorValue(value = "simple")
	public static class TestNode {
		@Id
		private Integer id;

		private String name;

		public TestNode() {
		}

		public TestNode(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity
	@DiscriminatorValue("composite")
	public static class TestCompositeNode extends TestNode {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "parent_id")
		private TestNode parent;

		public TestCompositeNode() {
		}

		public TestCompositeNode(Integer id, String name) {
			super( id, name );
		}

		public TestNode getParent() {
			return parent;
		}

		public void setParent(TestNode parent) {
			this.parent = parent;
		}
	}
}
