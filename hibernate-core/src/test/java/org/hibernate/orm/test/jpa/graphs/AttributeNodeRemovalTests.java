/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.graphs;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Subgraph;
import jakarta.persistence.Table;
import org.hibernate.Hibernate;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.NoSuchElementException;

import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.FetchType.LAZY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {
		AttributeNodeRemovalTests.Post.class,
		AttributeNodeRemovalTests.Person.class,
		AttributeNodeRemovalTests.Forum.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-19567")
public class AttributeNodeRemovalTests {
	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Person me = new Person( 1, "me" );
			final Person you = new Person( 2, "you" );
			final Person he = new Person( 3, "he" );
			session.persist( me );
			session.persist( you );
			session.persist( he );

			final Forum fiction = new Forum( 1, "Fiction" );
			final Forum nonFiction = new Forum( 2, "Non-Fiction" );
			session.persist( fiction );
			session.persist( nonFiction );

			session.persist( new Post( 1, "A Tale of Fairy", "Once upon a time...", fiction, you ) );
			session.persist( new Post( 2, "The Meaning of Life", "You only get one shot...", nonFiction, he ) );
			session.persist( new Post( 3, "Tales From Seaside", "The sun will come out...", fiction, me ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void testGraphBaseline(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var entityGraph = session.createEntityGraph( Post.class );
			var post = session.find( entityGraph, 1 );

			assertThat( Hibernate.isPropertyInitialized( post, "author" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getAuthor() ) ).isTrue();

			assertThat( Hibernate.isPropertyInitialized( post, "forum" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getForum() ) ).isFalse();
		} );

		factoryScope.inTransaction( (session) -> {
			var entityGraph = session.createEntityGraph( Post.class );
			var post = session.find( entityGraph, 1, GraphSemantic.LOAD );

			assertThat( Hibernate.isPropertyInitialized( post, "author" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getAuthor() ) ).isTrue();

			assertThat( Hibernate.isPropertyInitialized( post, "forum" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getForum() ) ).isFalse();
		} );

		factoryScope.inTransaction( (session) -> {
			var entityGraph = session.createEntityGraph( Post.class );
			var post = session.find( entityGraph, 1, GraphSemantic.FETCH );

			assertThat( Hibernate.isPropertyInitialized( post, "author" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getAuthor() ) ).isFalse();

			assertThat( Hibernate.isPropertyInitialized( post, "forum" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getForum() ) ).isFalse();
		} );
	}

	@Test
	void testAddLazyAttribute(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var entityGraph = session.createEntityGraph( Post.class );
			entityGraph.addAttributeNodes( "forum" );
			var post = session.find( entityGraph, 1 );

			assertThat( Hibernate.isPropertyInitialized( post, "author" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getAuthor() ) ).isTrue();

			assertThat( Hibernate.isPropertyInitialized( post, "forum" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getForum() ) ).isTrue();
		} );

		factoryScope.inTransaction( (session) -> {
			var entityGraph = session.createEntityGraph( Post.class );
			entityGraph.addAttributeNodes( "forum" );
			var post = session.find( entityGraph, 1, GraphSemantic.LOAD );

			assertThat( Hibernate.isPropertyInitialized( post, "author" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getAuthor() ) ).isTrue();

			assertThat( Hibernate.isPropertyInitialized( post, "forum" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getForum() ) ).isTrue();
		} );

		factoryScope.inTransaction( (session) -> {
			var entityGraph = session.createEntityGraph( Post.class );
			entityGraph.addAttributeNodes( "forum" );
			var post = session.find( entityGraph, 1, GraphSemantic.FETCH );

			assertThat( Hibernate.isPropertyInitialized( post, "author" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getAuthor() ) ).isFalse();

			assertThat( Hibernate.isPropertyInitialized( post, "forum" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getForum() ) ).isTrue();
		} );
	}

	@Test
	void testRemoveLazyAttribute(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var entityGraph = session.createEntityGraph( Post.class );
			entityGraph.removeAttributeNode( "forum" );
			var post = session.find( entityGraph, 1 );

			assertThat( Hibernate.isPropertyInitialized( post, "author" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getAuthor() ) ).isTrue();

			assertThat( Hibernate.isPropertyInitialized( post, "forum" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getForum() ) ).isFalse();
		} );

		factoryScope.inTransaction( (session) -> {
			var entityGraph = session.createEntityGraph( Post.class );
			entityGraph.removeAttributeNode( "forum" );
			var post = session.find( entityGraph, 1, GraphSemantic.LOAD );

			assertThat( Hibernate.isPropertyInitialized( post, "author" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getAuthor() ) ).isTrue();

			assertThat( Hibernate.isPropertyInitialized( post, "forum" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getForum() ) ).isFalse();
		} );

		factoryScope.inTransaction( (session) -> {
			var entityGraph = session.createEntityGraph( Post.class );
			entityGraph.removeAttributeNode( "forum" );
			var post = session.find( entityGraph, 1, GraphSemantic.FETCH );

			assertThat( Hibernate.isPropertyInitialized( post, "author" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getAuthor() ) ).isFalse();

			assertThat( Hibernate.isPropertyInitialized( post, "forum" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getForum() ) ).isFalse();
		} );
	}

	@Test
	void testAddEagerAttribute(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var entityGraph = session.createEntityGraph( Post.class );
			entityGraph.addAttributeNode( "author" );
			var post = session.find( entityGraph, 1 );

			assertThat( Hibernate.isPropertyInitialized( post, "author" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getAuthor() ) ).isTrue();

			assertThat( Hibernate.isPropertyInitialized( post, "forum" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getForum() ) ).isFalse();
		} );

		factoryScope.inTransaction( (session) -> {
			var entityGraph = session.createEntityGraph( Post.class );
			entityGraph.addAttributeNode( "author" );
			var post = session.find( entityGraph, 1, GraphSemantic.LOAD );

			assertThat( Hibernate.isPropertyInitialized( post, "author" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getAuthor() ) ).isTrue();

			assertThat( Hibernate.isPropertyInitialized( post, "forum" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getForum() ) ).isFalse();
		} );

		factoryScope.inTransaction( (session) -> {
			var entityGraph = session.createEntityGraph( Post.class );
			entityGraph.addAttributeNode( "author" );
			var post = session.find( entityGraph, 1, GraphSemantic.FETCH );

			assertThat( Hibernate.isPropertyInitialized( post, "author" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getAuthor() ) ).isTrue();

			assertThat( Hibernate.isPropertyInitialized( post, "forum" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getForum() ) ).isFalse();
		} );
	}

	@Test
	void testRemoveEagerAttribute(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var entityGraph = session.createEntityGraph( Post.class );
			entityGraph.removeAttributeNode( "author" );
			var post = session.find( entityGraph, 1 );

			assertThat( Hibernate.isPropertyInitialized( post, "author" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getAuthor() ) ).isFalse();

			assertThat( Hibernate.isPropertyInitialized( post, "forum" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getForum() ) ).isFalse();
		} );

		factoryScope.inTransaction( (session) -> {
			var entityGraph = session.createEntityGraph( Post.class );
			entityGraph.removeAttributeNode( "author" );
			var post = session.find( entityGraph, 1, GraphSemantic.LOAD );

			assertThat( Hibernate.isPropertyInitialized( post, "author" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getAuthor() ) ).isFalse();

			assertThat( Hibernate.isPropertyInitialized( post, "forum" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getForum() ) ).isFalse();
		} );

		factoryScope.inTransaction( (session) -> {
			var entityGraph = session.createEntityGraph( Post.class );
			entityGraph.removeAttributeNode( "author" );
			var post = session.find( entityGraph, 1, GraphSemantic.FETCH );

			// here we should NOT fall back to mapped fetch strategy (default to LAZY)
			assertThat( Hibernate.isPropertyInitialized( post, "author" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getAuthor() ) ).isFalse();

			assertThat( Hibernate.isPropertyInitialized( post, "forum" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getForum() ) ).isFalse();
		} );
	}

	@Test
	void testGraphApi(SessionFactoryScope factoryScope) {
		final RootGraphImplementor<Post> entityGraph = factoryScope.getSessionFactory().createEntityGraph( Post.class );

		// assertions based on the empty
		assertThat( entityGraph.getAttributeNodes() ).isEmpty();
		assertThat( entityGraph.getAttributeNodeList() ).isEmpty();
		assertThat( entityGraph.getNodes() ).isEmpty();
		assertThatThrownBy( () -> entityGraph.getAttributeNode( "author" ) )
				.isInstanceOf( NoSuchElementException.class );
		assertThatThrownBy( () -> entityGraph.getAttributeNode( "forum" ) )
				.isInstanceOf( NoSuchElementException.class );
		assertThat( entityGraph.findNode( "author" ) ).isNull();
		assertThat( entityGraph.findNode( "forum" ) ).isNull();

		// assertions based on the added "author" node
		entityGraph.addAttributeNode( "author" );
		assertThat( entityGraph.getAttributeNodes() ).isNotEmpty();
		assertThat( entityGraph.getAttributeNodeList() ).isNotEmpty();
		assertThat( entityGraph.getNodes() ).isNotEmpty();
		assertThat( entityGraph.getAttributeNode( "author" ) ).isNotNull();
		assertThatThrownBy( () -> entityGraph.getAttributeNode( "forum" ) )
				.isInstanceOf( NoSuchElementException.class );
		assertThat( entityGraph.findNode( "author" ) ).isNotNull();
		assertThat( entityGraph.findNode( "author" ).isRemoved() ).isFalse();
		assertThat( entityGraph.findNode( "author" ).getOptions() )
				.containsExactlyInAnyOrder( EAGER );
		assertThat( entityGraph.findNode( "forum" ) ).isNull();

		// assertions based on the removed "author" node
		entityGraph.removeAttributeNode( "author" );
		assertThat( entityGraph.getAttributeNodes() ).isNotEmpty();
		assertThat( entityGraph.getAttributeNodeList() ).isNotEmpty();
		assertThat( entityGraph.getNodes() ).isNotEmpty();
		assertThat( entityGraph.getAttributeNode( "author" ) ).isNotNull();
		assertThatThrownBy( () -> entityGraph.getAttributeNode( "forum" ) )
				.isInstanceOf( NoSuchElementException.class );
		assertThat( entityGraph.findNode( "author" ) ).isNotNull();
		assertThat( entityGraph.findNode( "author" ).isRemoved() ).isTrue();
		assertThat( entityGraph.findNode( "author" ).getOptions() )
				.containsExactlyInAnyOrder( LAZY );
		assertThat( entityGraph.findNode( "forum" ) ).isNull();
	}

	@Test
	void testFetchTypeOptions(SessionFactoryScope factoryScope) {
		final RootGraphImplementor<Post> entityGraph = factoryScope.getSessionFactory().createEntityGraph( Post.class );

		final var authorNode = entityGraph.addAttributeNode( "author" );
		assertThat( authorNode.getOptions() ).containsExactlyInAnyOrder( EAGER );

		authorNode.getOptions().clear();
		assertThat( authorNode.getOptions() ).containsExactlyInAnyOrder( EAGER );

		authorNode.addOption( FetchType.LAZY );
		assertThat( authorNode.getOptions() ).containsExactlyInAnyOrder( FetchType.LAZY );
		assertThat( entityGraph.getAttributeNode( "author" ) ).isNotNull();
		assertThat( entityGraph.findNode( "author" ).isRemoved() ).isTrue();

		authorNode.addOption( EAGER );
		assertThat( authorNode.getOptions() ).containsExactlyInAnyOrder( EAGER );
		assertThat( entityGraph.getAttributeNode( "author" ) ).isSameAs( authorNode );

		authorNode.addOption( FetchType.DEFAULT );
		assertThat( authorNode.getOptions() ).containsExactlyInAnyOrder( FetchType.DEFAULT );
		assertThat( entityGraph.findNode( "author" ).isRemoved() ).isFalse();
	}

	@Test
	void testLazyFetchTypeOption(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var entityGraph = session.createEntityGraph( Post.class );
			entityGraph.addAttributeNode( "author" ).addOption( FetchType.LAZY );
			var post = session.find( entityGraph, 1 );

			assertThat( Hibernate.isPropertyInitialized( post, "author" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getAuthor() ) ).isFalse();
		} );

		factoryScope.inTransaction( (session) -> {
			var entityGraph = session.createEntityGraph( Post.class );
			entityGraph.addAttributeNode( "author" ).addOption( FetchType.LAZY );
			var post = session.find( entityGraph, 1, GraphSemantic.FETCH );

			assertThat( Hibernate.isPropertyInitialized( post, "author" ) ).isTrue();
			assertThat( Hibernate.isInitialized( post.getAuthor() ) ).isFalse();
		} );
	}

	@Test
	void testAddRemoveOptionsCancel(SessionFactoryScope factoryScope) {
		final RootGraphImplementor<Post> entityGraph = factoryScope.getSessionFactory().createEntityGraph( Post.class );

		entityGraph.removeAttributeNode( "author" );
		assertThat( entityGraph.getAttributeNodes() ).hasSize( 1 );
		assertThat( entityGraph.findNode( "author" ).getOptions() )
				.containsExactlyInAnyOrder( LAZY );

		entityGraph.addAttributeNode( "author" );
		assertThat( entityGraph.getAttributeNodes() ).hasSize( 1 );
		assertThat( entityGraph.findNode( "author" ).getOptions() )
				.containsExactlyInAnyOrder( EAGER );

		entityGraph.removeAttributeNode( "author" );
		assertThat( entityGraph.getAttributeNodes() ).hasSize( 1 );
		assertThat( entityGraph.findNode( "author" ).getOptions() )
				.containsExactlyInAnyOrder( LAZY );
	}

	@Test
	void testRemoveAttributeNodeClearsSubgraph(SessionFactoryScope factoryScope) {
		final RootGraphImplementor<Post> entityGraph = factoryScope.getSessionFactory().createEntityGraph( Post.class );

		entityGraph.addSubgraph( "author" ).addAttributeNode( "name" );
		var author = entityGraph.findNode( "author" );
		assertThat( author.getOptions() ).containsExactlyInAnyOrder( EAGER );
		assertThat( author.getSubGraphs() ).isNotEmpty();

		entityGraph.removeAttributeNode( "author" );
		assertThat( entityGraph.getAttributeNodes() ).isNotEmpty();
		author = entityGraph.findNode( "author" );
		assertThat( author.getOptions() ).containsExactlyInAnyOrder( LAZY );
		assertThat( author.getSubGraphs() ).isEmpty();
	}

	@Test
	void testAttributeNodeAddSubgraphCancelsRemoval(SessionFactoryScope factoryScope) {
		final RootGraphImplementor<Post> entityGraph = factoryScope.getSessionFactory().createEntityGraph( Post.class );

		entityGraph.removeAttributeNode( "author" );
		jakarta.persistence.AttributeNode<Person> authorNode = entityGraph.getAttributeNode( "author" );
		assertThat( authorNode.getOptions() ).containsExactlyInAnyOrder( LAZY );

		Subgraph<Person> authorSubgraph = authorNode.addSubgraph();
		authorSubgraph.addAttributeNode( "name" );

		assertThat( authorNode.getOptions() ).containsExactlyInAnyOrder( EAGER );
		assertThat( authorNode.getSubgraphs() ).containsKey( Person.class );
	}

	@Entity(name="Person")
	@Table(name="persons")
	public static class Person {
		@Id
		private Integer id;
		private String name;

		protected Person() {
		}

		public Person(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name="Forum")
	@Table(name="forums")
	public static class Forum {
		@Id
		private Integer id;
		private String name;

		protected Forum() {
		}

		public Forum(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name="Post")
	@Table(name="posts")
	public static class Post {
		@Id
		private Integer id;
		private String title;
		@Lob
		private String body;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="forum_fk")
		private Forum forum;
		@ManyToOne(fetch = EAGER)
		@JoinColumn(name="author_fk")
		private Person author;

		protected Post() {
		}

		public Post(Integer id, String title, String body, Forum forum, Person author) {
			this.id = id;
			this.title = title;
			this.body = body;
			this.forum = forum;
			this.author = author;
		}

		public Integer getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getBody() {
			return body;
		}

		public void setBody(String body) {
			this.body = body;
		}

		public Forum getForum() {
			return forum;
		}

		public void setForum(Forum forum) {
			this.forum = forum;
		}

		public Person getAuthor() {
			return author;
		}

		public void setAuthor(Person author) {
			this.author = author;
		}
	}
}
