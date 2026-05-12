/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.proof;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.cfg.BatchSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@Jira( "https://hibernate.atlassian.net/browse/HHH-17529" )
// MySQLDialect and a few others do not enable batching by default
@ServiceRegistry(settings = @Setting(name= BatchSettings.STATEMENT_BATCH_SIZE, value="15"))
@DomainModel(annotatedClasses = {
		InsertOrderingWithSelfManyToOneAndChildrenManyToOneEntities.Post.class,
		InsertOrderingWithSelfManyToOneAndChildrenManyToOneEntities.PostComment.class
})
@SessionFactory(useCollectingStatementObserver = true)
public class InsertOrderingWithSelfManyToOneAndChildrenManyToOneEntities {
	@Test
	void testBatching(SessionFactoryScope factoryScope) {
		var sqlCollector = factoryScope.getCollectingStatementObserver();

		factoryScope.inTransaction( (session) -> {
			var parentPost = new Post( 1, "parent" );

			var parentPostCommentA = new PostComment( 10, "sdvewvr", parentPost );
			var parentPostCommentB = new PostComment( 11, "sdfverofivk", parentPost);

			var childPostA = new Post( 2, "child a", parentPost );
			var childApostComment = new PostComment( 20, "cedkvpd", childPostA );

			var childPostB = new Post( 3, "child b", parentPost );

			var childBpostComment = new PostComment( 30, "sdowkdf", childPostB );

			session.persist( parentPost );

			session.persist( parentPostCommentA );
			session.persist( parentPostCommentB );

			session.persist( childPostA );
			session.persist( childPostB );

			session.persist( childApostComment );
			session.persist( childBpostComment );

			sqlCollector.clear();
		} );

//			verifyContainsBatches(
//					new Batch( "insert into PostComment (post_ID,text,ID) values (?,?,?)", 4 ),
//					new Batch( "insert into Post (name,parent_ID,ID) values (?,?,?)", 3 )
//			);

		// Batch #1
		assertThat( sqlCollector.getStatements().get( 0 ).sql() ).startsWith( "insert into Post " );
		assertThat( sqlCollector.getStatements().get( 0 ).batchPosition() ).isEqualTo(1 );
		assertThat( sqlCollector.getStatements().get( 1 ).sql() ).startsWith( "insert into Post " );
		assertThat( sqlCollector.getStatements().get( 1 ).batchPosition() ).isEqualTo(2 );
		assertThat( sqlCollector.getStatements().get( 2 ).sql() ).startsWith( "insert into Post " );
		assertThat( sqlCollector.getStatements().get( 2 ).batchPosition() ).isEqualTo(3 );

		// Batch #2
		assertThat( sqlCollector.getStatements().get( 3 ).sql() ).startsWith( "insert into PostComment " );
		assertThat( sqlCollector.getStatements().get( 3 ).batchPosition() ).isEqualTo(1 );
		assertThat( sqlCollector.getStatements().get( 4 ).sql() ).startsWith( "insert into PostComment " );
		assertThat( sqlCollector.getStatements().get( 4 ).batchPosition() ).isEqualTo(2 );
		assertThat( sqlCollector.getStatements().get( 5 ).sql() ).startsWith( "insert into PostComment " );
		assertThat( sqlCollector.getStatements().get( 5 ).batchPosition() ).isEqualTo(3 );
		assertThat( sqlCollector.getStatements().get( 6 ).sql() ).startsWith( "insert into PostComment " );
		assertThat( sqlCollector.getStatements().get( 6 ).batchPosition() ).isEqualTo(4 );


		factoryScope.inTransaction( (session) -> {
			var posts = session.createQuery( "from Post", Post.class ).getResultList();
			assertThat( posts ).hasSize( 3 );
			var comments = session.createQuery( "from PostComment", PostComment.class ).getResultList();
			assertThat( comments ).hasSize( 4 );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Entity(name = "PostComment")
	public static class PostComment {
		@Id
		private Integer id;
		private String text;
		@ManyToOne
		private Post post;

		public PostComment() {
		}

		public PostComment(Integer id, String text, Post post) {
			this.id = id;
			this.text = text;
			this.post = post;
		}
	}

	@Entity(name = "Post")
	public static class Post {
		@Id
		private Integer id;
		private String name;
		@ManyToOne
		private Post parent;

		public Post() {
		}

		public Post(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Post(Integer id, String name, Post parent) {
			this.id = id;
			this.name = name;
			this.parent = parent;
		}
	}
}
