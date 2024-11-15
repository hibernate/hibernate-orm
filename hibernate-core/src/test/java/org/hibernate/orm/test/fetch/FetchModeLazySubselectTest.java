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
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@Jpa(
		annotatedClasses = {
				FetchModeLazySubselectTest.LazySelectRoot.class,
				FetchModeLazySubselectTest.LazySelectNode.class
		}
)
public class FetchModeLazySubselectTest {

	@BeforeAll
	static void setup(final EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
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
	void testSubselectFetching(final EntityManagerFactoryScope scope) {
		final LazySelectRoot root = scope.fromTransaction( em -> {
			final SessionImplementor session = em.unwrap( SessionImplementor.class );
			final LazySelectRoot in = em.find( LazySelectRoot.class, 1L );
			Hibernate.initialize( in.nodes );
			final EntityKey key1 = session.generateEntityKey( 10,
					session.getEntityPersister( null, new LazySelectNode() ) );
			final EntityKey key2 = session.generateEntityKey( 20,
					session.getEntityPersister( null, new LazySelectNode() ) );
			final EntityKey key3 = session.generateEntityKey( 30,
					session.getEntityPersister( null, new LazySelectNode() ) );
			assertThat( session.getPersistenceContextInternal().getBatchFetchQueue().getSubselect( key1 ),
					is( notNullValue() ) );
			assertThat( session.getPersistenceContextInternal().getBatchFetchQueue().getSubselect( key2 ),
					is( notNullValue() ) );
			assertThat( session.getPersistenceContextInternal().getBatchFetchQueue().getSubselect( key3 ),
					is( notNullValue() ) );
			return in;
		} );

		for ( final LazySelectNode node : root.nodes ) {
			assertThat( Hibernate.isInitialized( ((LazySelectNode) Hibernate.unproxy( node )).children ), is( false ) );
		}
	}

	@Entity(name = "root")
	public static class LazySelectRoot {
		@Id
		public Integer id;

		/**
		 * FIXME: Upon setting fetchtype to LAZY the subselects are created because they are loaded in a
		 * subsequent load event. If they are joinfetched using EAGER they are not. I suspect this might be wrong?
		 */
		@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "root")
		public Set<LazySelectNode> nodes = new HashSet<>();
	}

	@Entity(name = "node")
	public static class LazySelectNode {

		@Id
		public Integer id;

		@ManyToOne(cascade = CascadeType.ALL)
		public LazySelectRoot root;

		@ManyToMany(cascade = CascadeType.ALL)
		@JoinTable(name = "RELATIONSHIPS")
		@Fetch(FetchMode.SUBSELECT)
		public final Set<LazySelectNode> children = new HashSet<>();
	}
}
