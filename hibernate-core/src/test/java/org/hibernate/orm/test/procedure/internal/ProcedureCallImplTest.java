/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.procedure.internal;

import java.util.stream.Stream;
import jakarta.persistence.Query;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

/**
 * @author Nathan Xu
 */
@Jpa
public class ProcedureCallImplTest {

	@Test
	@JiraKey( value = "HHH-13644" )
	@RequiresDialect( H2Dialect.class )
	public void testNoNullPointerExceptionThrown(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			em.createNativeQuery("CREATE ALIAS GET_RANDOM_VALUE FOR \"java.lang.Math.random\";").executeUpdate();
			Query query = em.createStoredProcedureQuery("GET_RANDOM_VALUE");
			try (Stream stream = query.getResultStream()) {
				Assert.assertEquals( 1, stream.count() );
			}
		} );
	}
}
