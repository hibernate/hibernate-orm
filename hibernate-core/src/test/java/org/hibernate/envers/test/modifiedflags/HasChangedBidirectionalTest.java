/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-7949")
public class HasChangedBidirectionalTest extends AbstractModifiedFlagsEntityTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Ticket.class, Comment.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1 | Create ticket with comments
				entityManager -> {
					final Ticket ticket = new Ticket( 1, "data-t1" );
					final Comment comment = new Comment( 1, "Initial comment-t1" );
					ticket.addComment( comment );
					entityManager.persist( comment );
					entityManager.persist( ticket );
				},

				// Revision 2 | Create ticket without comments
				entityManager -> {
					final Ticket ticket = new Ticket( 2, "data-t2" );
					entityManager.persist( ticket );
				},

				// Revision 3 | Update ticket with comments
				entityManager -> {
					final Ticket ticket = entityManager.find( Ticket.class, 1 );
					ticket.setData( "data-changed-t1" );
					entityManager.merge( ticket );
				},

				// Revision 4 | Update ticket without comments
				entityManager -> {
					final Ticket ticket = entityManager.find( Ticket.class, 2 );
					ticket.setData( "data-changed-t2" );
					entityManager.merge( ticket );
				},

				// Revision 5 | Update ticket and comment
				entityManager -> {
					final Ticket ticket = entityManager.find( Ticket.class, 1 );
					ticket.setData( "data-changed-twice" );
					ticket.getComments().get( 0 ).setText( "comment-modified" );
					ticket.getComments().forEach( entityManager::merge );
					entityManager.merge( ticket );
				},

				// Revision 6 | Update ticket and comment collection
				entityManager -> {
					final Ticket ticket = entityManager.find( Ticket.class, 1 );
					final Comment comment = new Comment( 2, "Comment2" );
					ticket.addComment( comment );
					entityManager.merge( comment );
					entityManager.merge( ticket );
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( Ticket.class, 1 ), contains( 1, 3, 5, 6 ) );
		assertThat( getAuditReader().getRevisions( Ticket.class, 2 ), contains( 2, 4 ) );

		assertThat( getAuditReader().getRevisions( Comment.class, 1 ), contains( 1, 5 ) );
		assertThat( getAuditReader().getRevisions( Comment.class, 2 ), contains( 6 ) );
	}

	@DynamicTest
	public void testHasChanged() {
		assertThat( extractRevisions( queryForPropertyHasChanged( Ticket.class, 1, "comments" ) ), contains( 1, 6 ) );
		assertThat( extractRevisions( queryForPropertyHasChanged( Ticket.class, 2, "comments" ) ), contains( 2 ) );
	}

	@DynamicTest
	public void testHasNotChanged() {
		assertThat( extractRevisions( queryForPropertyHasNotChanged( Ticket.class, 1, "comments" ) ), contains( 3, 5 ) );
		assertThat( extractRevisions( queryForPropertyHasNotChanged( Ticket.class, 2, "comments" ) ), contains( 4 ) );
	}

	@Entity(name = "Ticket")
	@Audited(withModifiedFlag = true)
	public static class Ticket {
		@Id
		private Integer id;
		private String data;
		@OneToMany(mappedBy = "ticket")
		private List<Comment> comments = new ArrayList<>();

		Ticket() {

		}

		public Ticket(Integer id, String data) {
			this.id = id;
			this.data = data;
		}

		public Integer getId() {
			return id;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}

		public List<Comment> getComments() {
			return comments;
		}

		public void addComment(Comment comment) {
			comment.setTicket( this );
			comments.add( comment );
		}
	}

	@Entity(name = "Comment")
	@Table(name = "COMMENTS")
	@Audited(withModifiedFlag = true)
	public static class Comment {
		@Id
		private Integer id;
		@ManyToOne
		private Ticket ticket;
		private String text;

		Comment() {

		}

		public Comment(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}

		public Ticket getTicket() {
			return ticket;
		}

		public void setTicket(Ticket ticket) {
			this.ticket = ticket;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}
}
