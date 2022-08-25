/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embeddables;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@TestForIssue(jiraKey = "HHH-15453")
public class EmbeddableWithManyToManyTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Product.class,
				User.class
		};
	}

	@Test
	public void testMerge() {
		inTransaction(
				session -> {
					User user = new User( 1L, "Fab" );
					session.persist( user );

					Product product = new Product( 2L, "Sugar", new Users( user ) );
					Product mergedProduct = (Product) session.merge( product );
					assertThat( mergedProduct.getUsers().getUsers() ).isNotNull();
				}
		);

		inTransaction(
				session -> {
					Product product = session.get( Product.class, 2L );
					assertThat( product ).isNotNull();
					assertThat( product.getUsers().getUsers() ).isNotNull();
				}
		);
	}

	@Entity(name = "Product")
	public static class Product {
		@Id
		private Long id;

		private String name;

		@Embedded
		private Users users;

		public Product() {
		}

		public Product(Long id, String name, Users users) {
			this.id = id;
			this.name = name;
			this.users = users;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Users getUsers() {
			return users;
		}
	}

	@Embeddable
	public static class Users {

		@ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
		private Set<User> users;

		public Users() {
		}

		public Users(User... users) {
			this.users = Arrays.stream( users ).collect( Collectors.toSet() );
		}

		public Set<User> getUsers() {
			return users;
		}

		public void setUsers(Set<User> users) {
			this.users = users;
		}
	}

	@Entity(name = "User")
	@Table(name = "USER_TABLE")
	public static class User {
		@Id
		private Long id;

		private String name;

		public User() {
		}

		public User(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
