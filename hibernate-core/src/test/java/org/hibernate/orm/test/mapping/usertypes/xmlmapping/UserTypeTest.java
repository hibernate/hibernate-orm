/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.usertypes.xmlmapping;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

@Jpa(
		xmlMappings = "org/hibernate/orm/test/mapping/usertypes/xmlmapping/entities.xml"
)
@JiraKey("HHH-17262")
public class UserTypeTest {

	@Test
	public void testTypeAnnotationIsDetected(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Account account = new Account( "first", new AccountCurrencyUnit( "2", 0 ) );
					entityManager.persist( account );
				}
		);
	}
}
