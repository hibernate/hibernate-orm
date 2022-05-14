package org.hibernate.orm.test.onetoone.flush;

import java.util.List;
import java.util.Map;

import org.hibernate.CallbackException;
import org.hibernate.Interceptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.orm.test.jpa.SettingsGenerator;
import org.hibernate.type.Type;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.jpa.PersistenceUnitDescriptorAdapter;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.transaction.TransactionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

/**
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-15045" )
class DirtyFlushTest {

	List<Class> getAnnotatedClasses() {
		return List.of( User.class, Profile.class );
	}

	Map basicSettings() {
		return SettingsGenerator.generateSettings(
				AvailableSettings.HBM2DDL_AUTO, "create-drop",
				AvailableSettings.DIALECT, DialectContext.getDialect().getClass().getName(),
				AvailableSettings.LOADED_CLASSES, getAnnotatedClasses(),
				AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, "true"
		);
	}

	EntityManagerFactory buildEntityManagerFactory(Map settings) {
		return Bootstrap
				.getEntityManagerFactoryBuilder( new PersistenceUnitDescriptorAdapter(), settings )
				.build();
	}

	EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setUp() {
		DirtyFlushInterceptor.dirtyFlushedForUser = false;
	}

	@Test
	void testDirtyFlushNotHappened() {
		var settings = basicSettings();
		settings.put( AvailableSettings.INTERCEPTOR, new DirtyFlushInterceptor() );
		entityManagerFactory = buildEntityManagerFactory( settings );
		var em = entityManagerFactory.createEntityManager();

		TransactionUtil.inTransaction( em, entityManager -> {
			var user = new User();
			user.id = 1;
			entityManager.persist( user );
		} );

		try {
			TransactionUtil.inTransaction( em, entityManager -> {
				var user = entityManager.find( User.class, 1 );
				var profile = new Profile();
				profile.id = 1;
				profile.user = user;
				user.profile = profile;
				entityManager.persist( profile );
			} );

			Assertions.assertFalse( DirtyFlushInterceptor.dirtyFlushedForUser, "User should not be dirty-flushed when only Profile changes!" );

		} finally {
			TransactionUtil.inTransaction( em, entityManager -> {
				entityManager.createQuery( "delete from Profile " ).executeUpdate();
				entityManager.createQuery( "delete from User" ).executeUpdate();
			} );
		}
	}

	@AfterEach
	void releaseResources() {
		if ( entityManagerFactory != null ) {
			entityManagerFactory.close();
		}
	}


	@Entity(name = "User")
	static class User {
		@Id int id;

		@OneToOne(mappedBy = "user")
		Profile profile;
	}

	@Entity(name = "Profile")
	static class Profile {
		@Id int id;

		@OneToOne // internally Hibernate will use `@ManyToOne` for this field
		User user;
	}

	static class DirtyFlushInterceptor implements Interceptor {
		static boolean dirtyFlushedForUser;

		@Override
		public boolean onFlushDirty(
				Object entity,
				Object id,
				Object[] currentState,
				Object[] previousState,
				String[] propertyNames,
				Type[] types) throws CallbackException {

			System.out.println( "onFlushDirty invoked on entity: " + entity.getClass().getSimpleName() );

			dirtyFlushedForUser = entity instanceof DirtyFlushTest.User;

			return false;
		}
	}

}
