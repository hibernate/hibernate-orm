/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.annotations;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

@ServiceRegistry( settings = @Setting( name = AvailableSettings.URL, value = "jdbc:h2:mem:test_db" ) )
@DomainModel( annotatedClasses = AnEntity.class )
@SessionFactory
public class BasicSessionFactoryScopeTests {
	@Test
	public void testBasicUsage(SessionFactoryScope scope) {
		assertThat( scope, notNullValue() );
		assertThat( scope.getSessionFactory(), notNullValue() );
		// check we can use the SF to create Sessions
		scope.inTransaction(
				(session) -> session.createQuery( "from AnEntity" ).list()
		);
	}

}
