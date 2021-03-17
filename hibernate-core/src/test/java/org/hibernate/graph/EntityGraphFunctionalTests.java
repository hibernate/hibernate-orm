/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil2.inTransaction;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class EntityGraphFunctionalTests extends BaseEntityManagerFunctionalTestCase {

	@Test
	@TestForIssue( jiraKey = "HHH-13175")
	public void testSubsequentSelectFromFind() {
		inTransaction(
				entityManagerFactory(),
				session -> {
					final RootGraph<Issue> graph = GraphParser.parse( Issue.class, "comments", session );

					final Issue issue = session.find(
							Issue.class,
							1,
							Collections.singletonMap( GraphSemantic.LOAD.getJpaHintName(), graph )
					);

					assertTrue( Hibernate.isInitialized( issue ) );
					assertTrue( Hibernate.isInitialized( issue.getComments() ) );
					assertTrue( Hibernate.isInitialized( issue.getReporter() ) );
					assertTrue( Hibernate.isInitialized( issue.getAssignee() ) );

					assertFalse( Hibernate.isInitialized( issue.getAssignee().getAssignedIssues() ) );
				}
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { User.class, Issue.class, Comment.class };
	}

	@Override
	protected boolean createSchema() {
		return true;
	}

	@Before
	public void prepareTestData() {
		inTransaction(
				entityManagerFactory(),
				session -> {
					final User wesley = new User( "Wesley", "farmboy" );
					final User buttercup = new User( "Buttercup", "wishee" );
					final User humperdink = new User( "Humperdink", "buffoon" );

					session.save( wesley );
					session.save( buttercup );
					session.save( humperdink );

					final Issue flameSpurt = new Issue( 1, "Flame Spurt", wesley );
					final Issue lightningSand = new Issue( 2, "Lightning Sand", buttercup );
					final Issue rous = new Issue( 3, "Rodents of Unusual Size", wesley );

					flameSpurt.setAssignee( wesley );
					lightningSand.setAssignee( wesley );
					rous.setAssignee( wesley );

					session.save( flameSpurt );
					session.save( lightningSand );
					session.save( rous );

					flameSpurt.addComment( "There is a popping sound preceding each", wesley );
					rous.addComment( "I don't think they exist", wesley );
				}
		);
	}

	@After
	public void cleanUpTestData() {
		inTransaction(
				entityManagerFactory(),
				session -> {
					session.createQuery( "delete from Comment" ).executeUpdate();
					session.createQuery( "delete from Issue" ).executeUpdate();
					session.createQuery( "delete from User" ).executeUpdate();
				}
		);
	}

	@Entity( name = "Issue")
	@Table( name = "issue" )
	public static class Issue {
		private Integer id;
		private String description;
		private User reporter;
		private User assignee;
		private List<Comment> comments;

		public Issue() {
		}

		public Issue(Integer id, String description, User reporter) {
			this.id = id;
			this.description = description;
			this.reporter = reporter;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		@ManyToOne
		public User getReporter() {
			return reporter;
		}

		public void setReporter(User reporter) {
			this.reporter = reporter;
		}

		@ManyToOne
		public User getAssignee() {
			return assignee;
		}

		public void setAssignee(User assignee) {
			this.assignee = assignee;
		}

		@OneToMany( mappedBy = "issue", cascade = CascadeType.ALL)
		public List<Comment> getComments() {
			return comments;
		}

		public void setComments(List<Comment> comments) {
			this.comments = comments;
		}

		public void addComment(String comment, User user) {
			if ( comments == null ) {
				comments = new ArrayList<>();
			}

			comments.add( new Comment( this, comment, user ) );
		}
	}

	@Entity( name = "User")
	@Table( name = "`user`" )
	public static class User {
		private Integer id;
		private String name;
		private String login;
		private Set<Issue> assignedIssues;

		public User() {
		}

		public User(String name, String login) {
			this.name = name;
			this.login = login;
		}

		@Id
		@GeneratedValue
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

		@OneToMany(mappedBy="assignee", fetch= FetchType.LAZY)
		public Set<Issue> getAssignedIssues() {
			return assignedIssues;
		}

		public void setAssignedIssues(Set<Issue> assignedIssues) {
			this.assignedIssues = assignedIssues;
		}
	}

	@Entity(name = "Comment")
	@Table(name = "CommentTable") // "Comment" reserved in Oracle
	public static class Comment {
		private Integer id;
		private Issue issue;
		private String text;
		private Instant addedOn;
		private User commenter;

		public Comment() {
		}

		public Comment(
				Issue issue,
				String text,
				User commenter) {
			this.issue = issue;
			this.text = text;
			this.commenter = commenter;

			this.addedOn = Instant.now();
		}

		@Id
		@GeneratedValue
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@ManyToOne
		@JoinColumn(name="issue_id", nullable=false)
		public Issue getIssue() {
			return issue;
		}

		public void setIssue(Issue issue) {
			this.issue = issue;
		}

		@Column( name = "`text`" )
		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public Instant getAddedOn() {
			return addedOn;
		}

		public void setAddedOn(Instant addedOn) {
			this.addedOn = addedOn;
		}

		@ManyToOne
		@JoinColumn(name="user_id", nullable=false)
		public User getCommenter() {
			return commenter;
		}

		public void setCommenter(User commenter) {
			this.commenter = commenter;
		}
	}

}
