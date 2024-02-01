/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.basic;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				SetAsBasicTest.Post.class
		}
)
@SessionFactory
public class SetAsBasicTest {

	@Test
	public void testPersist(SessionFactoryScope scope) {
		Integer postId = 1;
		scope.inTransaction(
				session -> {
					Set<String> tags = new HashSet<>();
					tags.add( "tag1" );

					Post post = new Post( postId, "post", tags );
					session.persist( post );
				}
		);

		scope.inTransaction(
				session -> {
					Post post = session.find( Post.class, postId );
					Set<String> tags = post.getTags();
					assertThat( tags ).isNotNull();
					assertThat( tags.size() ).isEqualTo( 1 );
					assertThat( tags.stream().findFirst().get() ).isEqualTo( "tag1" );
				}
		);
	}

	@Entity
	@Table(name = "post")
	public static class Post {
		@Id
		public Integer id;

		public String name;

		Set<String> tags;

		public Post() {
		}

		public Post(Integer id, String name, Set<String> tags) {
			this.id = id;
			this.name = name;
			this.tags = tags;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Set<String> getTags() {
			return tags;
		}
	}

}
