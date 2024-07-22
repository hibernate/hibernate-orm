/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.selectbeforeupdate;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.SelectBeforeUpdate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Chris Cranford
 */
@DomainModel(
		annotatedClasses = {
				MergeDetachedTest.Foo.class,
				MergeDetachedTest.Bar.class
		}
)
@SessionFactory
public class MergeDetachedTest {

	@Test
	@JiraKey("HHH-5908")
	public void testUpdateDetachedUnchanged(SessionFactoryScope scope) {
		final Bar bar = new Bar( 1, "Bar" );
		final Foo foo = new Foo( 1, "Foo", bar );

		// this should generate versions
		scope.inTransaction( session -> {
			session.persist( bar );
			session.persist( foo );
		} );

		// this shouldn't generate a new version.
		Foo merged = scope.fromTransaction( session ->
													session.merge( foo )
		);

		assertThat( bar.getVersion() ).isEqualTo( 0 );
		assertThat( merged.getVersion() ).isEqualTo( 0 );

		// this should generate a new version
		Foo merged2 = scope.fromTransaction( session -> {
			merged.setName( "FooChanged" );
			return session.merge( merged );
		} );

		assertThat( bar.getVersion() ).isEqualTo( 0 );
		assertThat( merged2.getVersion() ).isEqualTo( 1 );
	}

	@Test
	@JiraKey("HHH-5908")
	public void testUpdateDetachedChanged(SessionFactoryScope scope) {
		final Bar bar = new Bar( 2, "Bar" );
		final Foo foo = new Foo( 2, "Foo", bar );

		// this should generate versions
		scope.inTransaction( session -> {
			session.persist( bar );
			session.persist( foo );
		} );

		// this should generate a new version
		Foo merged = scope.fromTransaction( session -> {
			foo.setName( "FooChanged" );
			return session.merge( foo );
		} );

		assertThat( bar.getVersion() ).isEqualTo( 0 );
		assertThat( merged.getVersion() ).isEqualTo( 1 );
	}

	@Test
	@JiraKey("HHH-5908")
	public void testUpdateDetachedUnchangedAndChanged(SessionFactoryScope scope) {
		final Bar bar = new Bar( 3, "Bar" );
		final Foo foo = new Foo( 3, "Foo", bar );

		// this should generate versions
		scope.inTransaction( session -> {
			session.persist( bar );
			session.persist( foo );
		} );

		// this shouldn't generate a new version.
		Foo merged = scope.fromTransaction(
				session ->
						session.merge( foo )
		);

		// this should generate a new version
		Foo merged2 = scope.fromTransaction( session -> {
			merged.setName( "FooChanged" );
			return session.merge( merged );
		} );

		assertThat( bar.getVersion() ).isEqualTo( 0 );
		assertThat( merged2.getVersion() ).isEqualTo( 1 );
	}

	@Test
	@JiraKey("HHH-5908")
	public void testUpdateDetachedChangedAndUnchanged(SessionFactoryScope scope) {
		final Bar bar = new Bar( 4, "Bar" );
		final Foo foo = new Foo( 4, "Foo", bar );

		// this should generate versions
		scope.inTransaction( session -> {
			session.persist( bar );
			session.persist( foo );
		} );

		// this should generate a new version
		Foo merged = scope.fromTransaction( session -> {
			foo.setName( "FooChanged" );
			return session.merge( foo );
		} );

		// this shouldn't generate a new version.
		Foo merged2 = scope.fromTransaction(
				session ->
						session.merge( merged )
		);

		assertThat( bar.getVersion() ).isEqualTo( 0 );
		assertThat( merged2.getVersion() ).isEqualTo( 1 );
	}

	@Test
	@JiraKey("HHH-14319")
	public void testUpdateDetachedWithAttachedPersistentSet(SessionFactoryScope scope) {
		final Bar bar = new Bar( 5, "Bar" );
		final Set<Comment> comments = new HashSet<>();
		comments.add( new Comment( "abc", "me" ) );
		bar.comments = comments;

		// this should generate versions
		scope.inTransaction( session -> {
			session.persist( bar );
		} );

		final Bar loadedBar = scope.fromTransaction( session -> {
			// We set the comments to the hash set and leave it "dirty"
			Bar b = session.find( Bar.class, bar.getId() );
			b.comments = comments;

			// During flushing, the comments HashSet becomes the backing collection of new PersistentSet which replaces the old entry
			session.flush();

			// Replace the persistent collection with the backing collection in the field
			b.comments = comments;

			// It's vital that we try merging a detached instance
			session.detach( b );
			return session.merge( b );
		} );

		assertThat( loadedBar.comments.size() ).isEqualTo( 1 );
	}

	@Entity(name = "Foo")
	@Table(name = "FooSBU")
	@SelectBeforeUpdate
	public static class Foo {
		@Id
		private Integer id;
		private String name;
		@Version
		private Integer version;
		@ManyToOne
		@JoinColumn(updatable = false)
		private Bar bar;

		Foo() {

		}

		Foo(Integer id, String name, Bar bar) {
			this.id = id;
			this.name = name;
			this.bar = bar;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Bar getBar() {
			return bar;
		}

		public void setBar(Bar bar) {
			this.bar = bar;
		}

		public Integer getVersion() {
			return version;
		}

		public void setVersion(Integer version) {
			this.version = version;
		}
	}

	@Entity(name = "Bar")
	@Table(name = "BarSBU")
	public static class Bar {
		@Id
		private Integer id;
		private String name;
		@Version
		private Integer version;
		@ElementCollection
		private Set<Comment> comments;

		Bar() {

		}

		Bar(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Integer getVersion() {
			return version;
		}

		public void setVersion(Integer version) {
			this.version = version;
		}

		public Set<Comment> getComments() {
			return comments;
		}

		public void setComments(Set<Comment> comments) {
			this.comments = comments;
		}
	}

	@Embeddable
	@Table(name = "CommentSBU")
	public static class Comment {
		@Column(name = "bar_comment")
		private String comment;
		private String author;

		public Comment() {
		}

		public Comment(String comment, String author) {
			this.comment = comment;
			this.author = author;
		}

		public String getComment() {
			return comment;
		}

		public void setComment(String comment) {
			this.comment = comment;
		}

		public String getAuthor() {
			return author;
		}

		public void setAuthor(String author) {
			this.author = author;
		}
	}
}
