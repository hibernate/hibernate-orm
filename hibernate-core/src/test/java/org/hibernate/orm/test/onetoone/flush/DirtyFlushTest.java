/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.flush;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey(value = "HHH-15045")
@Jpa(
		annotatedClasses = {
				DirtyFlushTest.User.class,
				DirtyFlushTest.Profile.class
		}
)
public class DirtyFlushTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			final User user = new User();
			user.id = 1;
			user.version = 1;

			final Profile profile = new Profile();
			profile.id = 1;
			profile.version = 1;

			em.persist( user );
			em.persist( profile );
		} );
	}

	@Test
	public void testDirtyFlushNotHappened(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			final User user = em.find( User.class, 1 );
			assertEquals( 1, user.version );

			final Profile profile = em.find( Profile.class, 1 );
			assertEquals( 1, profile.version );

			profile.user = user;
			user.profile = profile;

			em.persist( profile );
		} );

		scope.inTransaction( em -> {
			final Profile profile = em.find( Profile.class, 1 );
			assertEquals( 2, profile.version );

			final User user = em.find( User.class, 1 );
			assertEquals(
					1,
					user.version,
					"without fixing, the version will be bumped due to erroneous dirty flushing"
					);
		} );
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Entity(name = "User")
	@Table(name = "USER_TABLE")
	public static class User {
		@Id
		int id;

		@Version
		int version;

		@OneToOne(mappedBy = "user")
		Profile profile;
	}

	@Entity(name = "Profile")
	@Table(name = "PROFILE_TABLE")
	public static class Profile {
		@Id
		int id;

		@Version
		int version;
		@OneToOne // internally Hibernate will use `@ManyToOne` for this field
		User user;
	}
}
