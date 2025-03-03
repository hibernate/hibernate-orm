/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
