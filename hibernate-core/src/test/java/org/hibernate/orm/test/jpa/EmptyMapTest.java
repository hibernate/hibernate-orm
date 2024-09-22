/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Réda Housni Alaoui
 */
@Jpa(annotatedClasses = {EmptyMapTest.User.class, EmptyMapTest.Identity.class, EmptyMapTest.UserIdentity.class})
public class EmptyMapTest {

	@BeforeEach
	public void setupData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> em.persist( new User( 1 ) ) );
	}

	@AfterEach
	public void cleanupData(EntityManagerFactoryScope scope) {
		scope.inTransaction(em -> em.createQuery( "delete from User u" ).executeUpdate() );
	}

	@Test
	@JiraKey(value = "HHH-18658")
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			assertThat( em.find( User.class, 1 ) ).isNotNull();
		} );
	}

	@Entity(name = "User")
	@Table(name = "user_tbl")
	public static class User {
		@Id
		private int id;
		private String name;

		@OneToMany(
				mappedBy = "user",
				fetch = FetchType.EAGER,
				cascade = CascadeType.ALL,
				orphanRemoval = true)
		@MapKeyJoinColumn
		private final Map<Identity, UserIdentity> userIdentityByIdentity = new HashMap<>();

		public User() {
		}

		public User(int id) {
			this.id = id;
		}
	}

	@Entity(name = "Identity")
	@Table(name = "identity_tbl")
	public static class Identity {
		@Id
		@GeneratedValue
		private int id;
		private String name;
	}

	@Entity(name = "UserIdentity")
	@Table(name = "user_identity_tbl")
	public static class UserIdentity {
		@Id
		@GeneratedValue
		private int id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(nullable = false, updatable = false)
		private User user;

		@OneToOne(fetch = FetchType.EAGER)
		@JoinColumn(nullable = false, updatable = false)
		private Identity identity;
	}

}
