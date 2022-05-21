package org.hibernate.orm.test.onetoone.flush;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;

/**
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-15045" )
@Jpa(
	annotatedClasses = {
		DirtyFlushTest.User.class,
		DirtyFlushTest.Profile.class
	},
	properties = @Setting(name = AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, value = "true")
)
class DirtyFlushTest {

	@BeforeEach
	void setUp(EntityManagerFactoryScope scope) {
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
	void testDirtyFlushNotHappened(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			final User user = em.find( User.class, 1 );
			final Profile profile = em.find( Profile.class, 1 );
			profile.user = user;
			user.profile = profile;

			em.persist( profile );
			em.flush();
		} );

		scope.inTransaction( em -> {
			final Profile profile = em.find( Profile.class, 1 );
			Assertions.assertEquals( 2, profile.version );

			final User user = em.find( User.class, 1 );
			Assertions.assertEquals( 1, user.version, "without fixing, the version will be bumped due to erroneous dirty flushing" );
		} );
	}

	@AfterEach
	void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			em.createQuery( "delete from Profile" ).executeUpdate();
			em.createQuery( "delete from User" ).executeUpdate();
		} );
	}


	@Entity(name = "User")
	static class User {
		@Id
		int id;

		@Version
		int version;

		@OneToOne(mappedBy = "user")
		Profile profile;
	}

	@Entity(name = "Profile")
	static class Profile {
		@Id
		int id;

		@Version int version;
		@OneToOne // internally Hibernate will use `@ManyToOne` for this field
		User user;
	}

}
