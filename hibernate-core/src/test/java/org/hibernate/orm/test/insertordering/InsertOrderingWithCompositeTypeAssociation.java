/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-12355")
@Jpa(
		annotatedClasses = {
				InsertOrderingWithCompositeTypeAssociation.Book.class,
				InsertOrderingWithCompositeTypeAssociation.Comment.class
		},
		integrationSettings = {
				@Setting(name = AvailableSettings.ORDER_INSERTS, value = "true"),
				@Setting(name = AvailableSettings.ORDER_UPDATES, value = "true")
		}
)
public class InsertOrderingWithCompositeTypeAssociation {

	@Test
	public void testOrderedInsertSupport(EntityManagerFactoryScope scope) {
		// Without the fix, this transaction would eventually fail with a foreign-key constraint violation.
		//
		// The bookNoComment entity would be persisted just fine; however the bookWithComment would fail
		// because it would lead to inserting the Book entities first rather than making sure that the
		// Comment would be inserted first.
		//
		// The associated ActionQueue fix makes sure that regardless of the order of operations, the Comment
		// entity associated in the embeddable takes insert priority over the parent Book entity.
		scope.inTransaction( entityManager -> {
			Book bookNoComment = new Book();
			bookNoComment.setId( SafeRandomUUIDGenerator.safeRandomUUIDAsString() );

			Book bookWithComment = new Book();
			bookWithComment.setId( SafeRandomUUIDGenerator.safeRandomUUIDAsString() );
			bookWithComment.setIntermediateObject( new IntermediateObject( new Comment( "This is a comment" ) ) );

			entityManager.persist( bookNoComment );
			entityManager.persist( bookWithComment );
		} );
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		private String id;

		private String title;

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

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}

	@Entity(name = "Comment")
	@Table(name = "COMMENT_TABLE")
	public static class Comment {
		@Id
		private String id;
		@Column(name = "`comment`", length = 256)
		private String comment;

		Comment() {

		}

		Comment(String comment) {
			this.id = SafeRandomUUIDGenerator.safeRandomUUIDAsString();
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
		@JoinColumn(name = "comment_comment", foreignKey = @ForeignKey(name = "id"))
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

}
