/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.graph.GraphParser;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.hamcrest.InitializationCheckMatcher.isInitialized;
import static org.hibernate.testing.hamcrest.InitializationCheckMatcher.isNotInitialized;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				EntityGraphFunctionalTests.User.class,
				EntityGraphFunctionalTests.Issue.class,
				EntityGraphFunctionalTests.Comment.class
		}
)
@SessionFactory
public class EntityGraphFunctionalTests {

	@Test
	@JiraKey( value = "HHH-13175")
	void testSubsequentSelectFromFind(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final RootGraph<Issue> graph = GraphParser.parse( Issue.class, "comments", session );

					final Issue issue = session.find(
							Issue.class,
							1,
							Collections.singletonMap( GraphSemantic.LOAD.getJpaHintName(), graph )
					);

					assertThat( issue, isInitialized() );
					assertThat( issue.getComments(), isInitialized() );
					assertThat( issue.getReporter(), isInitialized() );
					assertThat( issue.getAssignee(), isInitialized() );

					assertThat( issue.getAssignee().getAssignedIssues(), isNotInitialized() );
				}
		);
	}

	@BeforeEach
	void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final User wesley = new User( "Wesley", "farmboy" );
					final User buttercup = new User( "Buttercup", "wishee" );
					final User humperdink = new User( "Humperdink", "buffoon" );

					session.persist( wesley );
					session.persist( buttercup );
					session.persist( humperdink );

					final Issue flameSpurt = new Issue( 1, "Flame Spurt", wesley );
					final Issue lightningSand = new Issue( 2, "Lightning Sand", buttercup );
					final Issue rous = new Issue( 3, "Rodents of Unusual Size", wesley );

					flameSpurt.setAssignee( wesley );
					lightningSand.setAssignee( wesley );
					rous.setAssignee( wesley );

					session.persist( flameSpurt );
					session.persist( lightningSand );
					session.persist( rous );

					flameSpurt.addComment( "There is a popping sound preceding each", wesley );
					rous.addComment( "I don't think they exist", wesley );
				}
		);
	}

	@AfterEach
	void cleanUpTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
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
