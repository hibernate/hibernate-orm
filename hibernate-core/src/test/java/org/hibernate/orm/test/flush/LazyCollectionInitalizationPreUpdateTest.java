/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@SessionFactory
@DomainModel( annotatedClasses = {
		LazyCollectionInitalizationPreUpdateTest.TreeNode.class,
		LazyCollectionInitalizationPreUpdateTest.ReferencedEntity.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16602" )
public class LazyCollectionInitalizationPreUpdateTest {
	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TreeNode first = new TreeNode( 1L, null );
			session.persist( first );
			final TreeNode second = new TreeNode( 2L, first );
			final ReferencedEntity referenced = new ReferencedEntity( "referenced" );
			session.persist( referenced );
			second.getSomeSet().add( referenced );
			session.persist( second );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from ReferencedEntity" ).executeUpdate() );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.getSessionFactory().getEventEngine().getListenerRegistry().appendListeners(
				EventType.PRE_UPDATE,
				Listener.class
		);
		scope.inTransaction( session -> {
			final TreeNode first = session.byId( TreeNode.class ).load( 1L );
			final TreeNode second = session.byId( TreeNode.class ).load( 2L );
			session.remove( first );
			session.remove( second );
		} );
	}

	@Entity( name = "TreeNode" )
	public static class TreeNode {
		@Id
		private Long id;

		@ManyToOne
		@JoinColumn( name = "parent_id" )
		private TreeNode parent;

		@OneToMany
		private Set<ReferencedEntity> someSet = new HashSet<>();

		public TreeNode() {
		}

		public TreeNode(Long id, TreeNode parent) {
			this.id = id;
			this.parent = parent;
		}

		public Set<ReferencedEntity> getSomeSet() {
			return someSet;
		}
	}

	@Entity( name = "ReferencedEntity" )
	public static class ReferencedEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public ReferencedEntity() {
		}

		public ReferencedEntity(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public static class Listener implements PreUpdateEventListener {
		@Override
		public boolean onPreUpdate(PreUpdateEvent event) {
			final Object entity = event.getEntity();
			if ( entity instanceof TreeNode ) {
				final TreeNode treeNode = (TreeNode) entity;
				assertThat( Hibernate.isInitialized( treeNode.getSomeSet() ) ).isFalse();
				treeNode.getSomeSet().forEach( entry -> {
					assertThat( entry.getName() ).isEqualTo( "referenced" );
				} );
				assertThat( Hibernate.isInitialized( treeNode.getSomeSet() ) ).isTrue();
			}
			return false;
		}
	}
}
