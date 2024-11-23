/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.selectbeforeupdate;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Version;

import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
public class UpdateDetachedTest extends BaseCoreFunctionalTestCase{

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Foo.class, Bar.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-5908")
	public void testUpdateDetachedUnchanged() {
		final Bar bar = new Bar( 1, "Bar" );
		final Foo foo = new Foo( 1, "Foo", bar );

		// this should generate versions
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.save( bar );
			session.save( foo );
		} );

		// this shouldn't generate a new version.
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.update( foo );
		} );

		assertEquals( Integer.valueOf( 0 ), bar.getVersion() );
		assertEquals( Integer.valueOf( 0 ), foo.getVersion() );

		// this should generate a new version
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			foo.setName( "FooChanged" );
			session.update( foo );
		} );

		assertEquals( Integer.valueOf( 0 ), bar.getVersion() );
		assertEquals( Integer.valueOf( 1 ), foo.getVersion() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-5908")
	public void testUpdateDetachedChanged() {
		final Bar bar = new Bar( 2, "Bar" );
		final Foo foo = new Foo( 2, "Foo", bar );

		// this should generate versions
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.save( bar );
			session.save( foo );
		} );

		// this should generate a new version
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			foo.setName( "FooChanged" );
			session.update( foo );
		} );

		assertEquals( Integer.valueOf( 0 ), bar.getVersion() );
		assertEquals( Integer.valueOf( 1 ), foo.getVersion() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-5908")
	public void testUpdateDetachedUnchangedAndChanged() {
		final Bar bar = new Bar( 3, "Bar" );
		final Foo foo = new Foo( 3, "Foo", bar );

		// this should generate versions
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.save( bar );
			session.save( foo );
		} );

		// this shouldn't generate a new version.
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.update( foo );
		} );

		// this should generate a new version
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			foo.setName( "FooChanged" );
			session.update( foo );
		} );

		assertEquals( Integer.valueOf( 0 ), bar.getVersion() );
		assertEquals( Integer.valueOf( 1 ), foo.getVersion() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-5908")
	public void testUpdateDetachedChangedAndUnchanged() {
		final Bar bar = new Bar( 4, "Bar" );
		final Foo foo = new Foo( 4, "Foo", bar );

		// this should generate versions
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.save( bar );
			session.save( foo );
		} );

		// this should generate a new version
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			foo.setName( "FooChanged" );
			session.update( foo );
		} );

		// this shouldn't generate a new version.
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.update( foo );
		} );

		assertEquals( Integer.valueOf( 0 ), bar.getVersion() );
		assertEquals( Integer.valueOf( 1 ), foo.getVersion() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14319")
	public void testUpdateDetachedWithAttachedPersistentSet() {
		final Bar bar = new Bar( 5, "Bar" );
		final Set<Comment> comments = new HashSet<>();
		comments.add( new Comment( "abc", "me" ) );
		bar.comments = comments;

		// this should generate versions
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.save( bar );
		} );
		final Bar loadedBar = TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			// We set the comments to the hash set and leave it "dirty"
			Bar b = session.find( Bar.class, bar.getId() );
			b.comments = comments;

			// During flushing, the comments HashSet becomes the backing collection of new PersistentSet which replaces the old entry
			session.flush();

			// Replace the persistent collection with the backing collection in the field
			b.comments = comments;

			// It's vital that we try merging a detached instance
			session.detach( b );
			return (Bar) session.merge( b );
		} );

		assertEquals( 1, loadedBar.comments.size() );
	}

	@Entity(name = "Foo")
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
