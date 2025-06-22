/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.basic;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
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
// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
@BootstrapServiceRegistry(integrators = SharedDriverManagerTypeCacheClearingIntegrator.class)
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
