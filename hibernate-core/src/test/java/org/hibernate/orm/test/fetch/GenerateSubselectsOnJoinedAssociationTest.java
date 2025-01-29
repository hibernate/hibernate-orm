/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetch;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.Hibernate;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfileOverride;
import org.hibernate.engine.spi.BatchFetchQueue;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@SessionFactory
@DomainModel(annotatedClasses = {
		GenerateSubselectsOnJoinedAssociationTest.LazySelectRoot.class,
		GenerateSubselectsOnJoinedAssociationTest.LazySelectNode.class
})
public class GenerateSubselectsOnJoinedAssociationTest {

	private EntityKey key1;
	private EntityKey key2;
	private EntityKey key3;

	@BeforeEach
	void setup(final SessionFactoryScope scope) {
		final EntityPersister persister = scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( LazySelectNode.class );

		key1 = new EntityKey( 10, persister );
		key2 = new EntityKey( 20, persister );
		key3 = new EntityKey( 30, persister );

		scope.inTransaction( em -> {
			if ( em.find( LazySelectRoot.class, 1 ) != null ) {
				return;
			}

			final LazySelectNode rootNode = new LazySelectNode();
			rootNode.id = 10;

			final LazySelectNode child1 = new LazySelectNode();
			child1.id = 20;

			final LazySelectNode child2 = new LazySelectNode();
			child2.id = 30;

			final LazySelectRoot root = new LazySelectRoot();
			root.id = 1;

			root.nodes.add( rootNode );
			root.nodes.add( child1 );
			root.nodes.add( child2 );
			rootNode.root = root;
			rootNode.children.add( child2 );
			rootNode.children.add( child1 );
			child1.root = root;
			child2.root = root;

			em.persist( root );
		} );
	}

	@Test
	void testLazySubselectFetchingFromFind(final SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFetchProfile( "lazysubselect" );

			final LazySelectRoot root = session.find( LazySelectRoot.class, 1 );

			assertNotInitialized( root );
			Hibernate.initialize( root.nodes );

			assertGeneratedSubselects( session );
			assertNodesInitialized( root );

			final LazySelectNode anyNode = root.nodes.iterator().next();
			Hibernate.initialize( anyNode.children );

			assertFullyInitialized( root );
		} );
	}

	@Test
	void testEagerSubselectFetchingFromFind(final SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFetchProfile( "eagersubselect" );

			final LazySelectRoot root = session.find( LazySelectRoot.class, 1 );

			assertGeneratedSubselects( session );
			assertFullyInitialized( root );
		} );
	}

	@Test
	void testLazySubselectFetchingFromQuery(final SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFetchProfile( "lazysubselect" );

			final LazySelectRoot root = session.createQuery( "select r from root r where r.id = ?1",
							LazySelectRoot.class )
					.setParameter( 1, 1 ).getSingleResult();

			assertNotInitialized( root );
			Hibernate.initialize( root.nodes );

			assertGeneratedSubselects( session );
			assertNodesInitialized( root );

			final LazySelectNode anyNode = root.nodes.iterator().next();
			Hibernate.initialize( anyNode.children );

			assertFullyInitialized( root );
		} );
	}

	@Test
	void testEagerSubselectFetchingFromQuery(final SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFetchProfile( "eagersubselect" );

			final LazySelectRoot root = session.createQuery( "select r from root r where r.id = ?1",
					LazySelectRoot.class ).setParameter( 1, 1 ).getSingleResult();

			assertGeneratedSubselects( session );
			assertFullyInitialized( root );
		} );
	}

	private void assertGeneratedSubselects(final SessionImplementor session) {
		final BatchFetchQueue batchFetchQueue = session.getPersistenceContextInternal().getBatchFetchQueue();
		assertThat( batchFetchQueue.getSubselect( key1 ), is( notNullValue() ) );
		assertThat( batchFetchQueue.getSubselect( key2 ), is( notNullValue() ) );
		assertThat( batchFetchQueue.getSubselect( key3 ), is( notNullValue() ) );
	}

	private void assertNotInitialized(LazySelectRoot root) {
		assertThat( Hibernate.isInitialized( root.getNodes() ), is( false ) );
	}

	private void assertNodesInitialized(LazySelectRoot root) {
		assertThat( Hibernate.isInitialized( root.getNodes() ), is( true ) );

		for ( final LazySelectNode node : root.getNodes() ) {
			assertThat( Hibernate.isInitialized( node.getChildren() ), is( false ) );
		}
	}

	private void assertFullyInitialized(LazySelectRoot root) {
		assertThat( Hibernate.isInitialized( root.getNodes() ), is( true ) );

		for ( final LazySelectNode node : root.getNodes() ) {
			assertThat( Hibernate.isInitialized( node.getChildren() ), is( true ) );
		}
	}

	@Entity(name = "root")
	@FetchProfile(name = "lazysubselect")
	@FetchProfile(name = "eagersubselect")
	public static class LazySelectRoot {
		@Id
		public Integer id;

		@OneToMany(cascade = CascadeType.ALL, mappedBy = "root")
		@FetchProfileOverride(profile = "lazysubselect", fetch = FetchType.LAZY)
		@FetchProfileOverride(profile = "eagersubselect", fetch = FetchType.EAGER)
		public Set<LazySelectNode> nodes = new HashSet<>();

		public Set<LazySelectNode> getNodes() {
			return nodes;
		}
	}

	@Entity(name = "node")
	public static class LazySelectNode {

		@Id
		public Integer id;

		@ManyToOne(cascade = CascadeType.ALL)
		public LazySelectRoot root;

		@ManyToMany(cascade = CascadeType.ALL)
		@JoinTable(name = "RELATIONSHIPS")
		@FetchProfileOverride(profile = "lazysubselect", fetch = FetchType.LAZY, mode = FetchMode.SUBSELECT)
		@FetchProfileOverride(profile = "eagersubselect", fetch = FetchType.EAGER, mode = FetchMode.SUBSELECT)
		public final Set<LazySelectNode> children = new HashSet<>();

		public Set<LazySelectNode> getChildren() {
			return children;
		}
	}
}
