/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import java.util.Map;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 *
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12355")
public class InsertOrderingWithCompositeTypeAssociation extends BaseCoreFunctionalTestCase {

	@Entity(name = "Book")
	public static class Book {
		@Id
		private String id;
		@Embedded
		private IntermediateObject intermediateObject;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public IntermediateObject getIntermediateObject() {
			return intermediateObject;
		}

		public void setIntermediateObject(IntermediateObject intermediateObject) {
			this.intermediateObject = intermediateObject;
		}
	}

	@Entity(name = "Comment")
	public static class Comment {
		@Id
		private String id;
		@Column(length = 256)
		private String comment;

		Comment() {

		}

		Comment(String comment) {
			this.id = UUID.randomUUID().toString();
			this.comment = comment;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getComment() {
			return comment;
		}

		public void setComment(String comment) {
			this.comment = comment;
		}
	}

	@Embeddable
	public static class IntermediateObject {
		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, targetEntity = Comment.class)
		@JoinColumn(name = "comment_comment", foreignKey = @ForeignKey(name = "id" ) )
		private Comment comment;

		IntermediateObject() {

		}

		IntermediateObject(Comment comment) {
			this.comment = comment;
		}

		public Comment getComment() {
			return comment;
		}

		public void setComment(Comment comment) {
			this.comment = comment;
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Book.class, Comment.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( AvailableSettings.ORDER_INSERTS, "true" );
		cfg.setProperty( AvailableSettings.ORDER_UPDATES, "true" );
	}

	@Test
	public void testOrderedInsertSupport() {
		// Without the fix, this transaction would eventually fail with a foreign-key constraint violation.
		//
		// The bookNoComment entity would be persisted just fine; however the bookWithComment would fail
		// because it would lead to inserting the Book entities first rather than making sure that the
		// Comment would be inserted first.
		//
		// The associated ActionQueue fix makes sure that regardless of the order of operations, the Comment
		// entity associated in the embeddable takes insert priority over the parent Book entity.
		Session session = openSession();
		session.getTransaction().begin();
		{
			Book bookNoComment = new Book();
			bookNoComment.setId( UUID.randomUUID().toString() );

			Book bookWithComment = new Book();
			bookWithComment.setId( UUID.randomUUID().toString() );
			bookWithComment.setIntermediateObject( new IntermediateObject( new Comment( "This is a comment" ) ) );

			session.persist( bookNoComment );
			session.persist( bookWithComment );
		}
		session.getTransaction().commit();
		session.close();
	}
}
