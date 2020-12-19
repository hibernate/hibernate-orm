package org.your.domain;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.arjuna.ats.internal.jdbc.recovery.JDBCXARecovery.PASSWORD;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.cfg.AvailableSettings.URL;
import static org.hibernate.cfg.AvailableSettings.USER;

/**
 * Tests for SimpleEntity
 */
@ServiceRegistry(
		settings = {
				// can define settings here, or in `hibernate.properties` file
				@Setting( name = URL, value = "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000" ),
				@Setting( name = USER, value = "sa" ),
				@Setting( name = PASSWORD, value = "" )
		}
)
@DomainModel( annotatedClasses = SimpleEntity.class )
@SessionFactory()
public class EntityTests {
	@Test
	public void basicTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final SimpleEntity entity = session.createQuery( "from SimpleEntity", SimpleEntity.class ).uniqueResult();
					assertThat( entity, notNullValue() );
				}
		);
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.persist( new SimpleEntity( 1, "the first" ) )
		);
	}

	@BeforeEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "delete SimpleEntity" ).executeUpdate()
		);
	}
}
