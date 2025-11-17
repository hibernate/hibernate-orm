/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.envers.tools.TestTools.extractRevisionNumbers;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-7949")
@Jpa(integrationSettings = @Setting(name = EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, value = "true"),
		annotatedClasses = {HasChangedBidirectionalTest.Ticket.class, HasChangedBidirectionalTest.Comment.class})
public class HasChangedBidirectionalTest extends AbstractModifiedFlagsEntityTest {

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1 | Create ticket with comments
		scope.inEntityManager( entityManager -> {
			entityManager.getTransaction().begin();
			final Ticket ticket = new Ticket( 1, "data-t1" );
			final Comment comment = new Comment( 1, "Initial comment-t1" );
			ticket.addComment( comment );
			entityManager.persist( comment );
			entityManager.persist( ticket );
			entityManager.getTransaction().commit();
		} );

		// Revision 2 | Create ticket without comments
		scope.inEntityManager( entityManager -> {
			entityManager.getTransaction().begin();
			final Ticket ticket = new Ticket( 2, "data-t2" );
			entityManager.persist( ticket );
			entityManager.getTransaction().commit();
		} );

		// Revision 3 | Update ticket with comments
		scope.inEntityManager( entityManager -> {
			entityManager.getTransaction().begin();
			final Ticket ticket = entityManager.find( Ticket.class, 1 );
			ticket.setData( "data-changed-t1" );
			entityManager.merge( ticket );
			entityManager.getTransaction().commit();
		} );

		// Revision 4 | Update ticket without comments
		scope.inEntityManager( entityManager -> {
			entityManager.getTransaction().begin();
			final Ticket ticket = entityManager.find( Ticket.class, 2 );
			ticket.setData( "data-changed-t2" );
			entityManager.merge( ticket );
			entityManager.getTransaction().commit();
		} );

		// Revision 5 | Update ticket and comment
		scope.inEntityManager( entityManager -> {
			entityManager.getTransaction().begin();
			final Ticket ticket = entityManager.find( Ticket.class, 1 );
			ticket.setData( "data-changed-twice" );
			ticket.getComments().get( 0 ).setText( "comment-modified" );
			ticket.getComments().forEach( entityManager::merge );
			entityManager.merge( ticket );
			entityManager.getTransaction().commit();
		} );

		// Revision 6 | Update ticket and comment collection
		scope.inEntityManager( entityManager -> {
			entityManager.getTransaction().begin();
			final Ticket ticket = entityManager.find( Ticket.class, 1 );
			final Comment comment = new Comment( 2, "Comment2" );
			ticket.addComment( comment );
			entityManager.merge( comment );
			entityManager.merge( ticket );
			entityManager.getTransaction().commit();
		} );
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 3, 5, 6 ), auditReader.getRevisions( Ticket.class, 1 ) );
			assertEquals( Arrays.asList( 2, 4 ), auditReader.getRevisions( Ticket.class, 2 ) );
			assertEquals( Arrays.asList( 1, 5 ), auditReader.getRevisions( Comment.class, 1 ) );
			assertEquals( Arrays.asList( 6 ), auditReader.getRevisions( Comment.class, 2 ) );
		} );
	}

	@Test
	public void testHasChanged(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 6 ), extractRevisionNumbers( queryForPropertyHasChanged( auditReader, Ticket.class, 1, "comments" ) ) );
			assertEquals( Arrays.asList( 2 ), extractRevisionNumbers( queryForPropertyHasChanged( auditReader, Ticket.class, 2, "comments" ) ) );
		} );
	}

	@Test
	public void testHasNotChanged(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 3, 5 ), extractRevisionNumbers( queryForPropertyHasNotChanged( auditReader, Ticket.class, 1, "comments" ) ) );
			assertEquals( Arrays.asList( 4 ), extractRevisionNumbers( queryForPropertyHasNotChanged( auditReader, Ticket.class, 2, "comments" ) ) );
		} );
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
