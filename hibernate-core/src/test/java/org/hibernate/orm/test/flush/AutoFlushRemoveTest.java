/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel( annotatedClasses = {
		AutoFlushRemoveTest.Item.class,
		AutoFlushRemoveTest.Comment.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-3354" )
public class AutoFlushRemoveTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Item first = new Item( 1L, "item" );
			session.persist( first );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Comment" ).executeUpdate();
			session.createMutationQuery( "delete from Item" ).executeUpdate();
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Item item = session.find( Item.class, 1L );

			final Comment comment1 = new Comment( "c1", item );
			session.persist( comment1 );
			final Comment comment2 = new Comment( "c2", item );
			session.persist( comment2 );
			item.getComments().add( comment1 );
			item.getComments().add( comment2 );

			session.flush();

			session.remove( comment1 );
			comment1.item = null;
		} );
		scope.inTransaction( session -> {
			final Item item = session.find( Item.class, 1L );
			assertThat( item.getComments().size() ).isEqualTo( 1 );
		});
	}

	@Entity( name = "Item" )
	public static class Item {
		@Id
		private Long id;
		private String name;
		@OneToMany(mappedBy = "item")
		private Set<Comment> comments = new HashSet<>();

		public Item() {
		}

		public Item(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public Set<Comment> getComments() {
			return comments;
		}
	}

	@Entity( name = "Comment" )
	@Table(name = "item_comment")
	public static class Comment {
		@Id
		@GeneratedValue
		private Long id;
		private String text;
		@ManyToOne(fetch = FetchType.LAZY)
		private Item item;

		public Comment() {
		}

		public Comment(String text, Item item) {
			this.text = text;
			this.item = item;
		}

		public String getText() {
			return text;
		}

		public Item getItem() {
			return item;
		}
	}
}
