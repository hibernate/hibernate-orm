/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.annotations;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

@RequiresDialect(H2Dialect.class)
@Jpa(
		annotatedClasses = {
				AnEntity.class
		}
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