/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.annotations;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

@Jpa(
		annotatedClasses = {
				AnEntity.class
		},
		properties = {
				@Setting( name = AvailableSettings.JPA_JDBC_URL, value = "jdbc:h2:mem:test_db" ),
				@Setting( name = AvailableSettings.JPA_JDBC_USER, value = "tester" )
		}
// works with either
//		integrationSettings = {
//				@Setting( name = AvailableSettings.JPA_JDBC_URL, value = "jdbc:h2:mem:test_db" ),
//				@Setting( name = AvailableSettings.JPA_JDBC_USER, value = "tester" )
//		}
)
public class BasicEntityManagerFactoryScopeTests {
	@Test
	public void testBasicUsage(EntityManagerFactoryScope scope) {
		assertThat( scope, notNullValue() );
		assertThat( scope.getEntityManagerFactory(), notNullValue() );
		// check we can use the EMF to create EMs
		scope.inTransaction(
				(session) -> session.createQuery( "select a from AnEntity a" ).getResultList()
		);
	}

}
