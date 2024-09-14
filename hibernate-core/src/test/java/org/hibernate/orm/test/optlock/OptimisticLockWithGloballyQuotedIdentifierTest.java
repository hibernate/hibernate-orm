/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.optlock;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Version;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


@DomainModel(
		annotatedClasses = {
				OptimisticLockWithGloballyQuotedIdentifierTest.Person.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting( name =  AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, value = "true")
)
public class OptimisticLockWithGloballyQuotedIdentifierTest {


	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person person = new Person( "1", "Fabiana" );
					session.persist( person );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Person" ).executeUpdate();
				}
		);
	}

	@Test
	public void testHqlQueryWithOptimisticLock(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from Person e", Person.class )
							.setLockMode( LockModeType.OPTIMISTIC )
							.getResultList().get( 0 );
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private String id;

		@Version
		private long version;

		private String name;

		public Person() {
		}

		public Person(String id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
