/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converter.hbm;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@Jpa(xmlMappings = {"org/hibernate/orm/test/mapping/converter/hbm/MoneyConverterHbmTest.hbm.xml"})
public class MoneyConverterHbmTest {

	@Test
	public void testConverterMutability(EntityManagerFactoryScope scope) {

		scope.inTransaction( entityManager -> {
			Account account = new Account();
			account.setId(1L);
			account.setOwner("John Doe");
			account.setBalance(new Money(250 * 100L));

			entityManager.persist(account);
		});

		scope.inTransaction( entityManager -> {
			//tag::basic-hbm-convert-money-converter-mutability-plan-example[]
			Account account = entityManager.find(Account.class, 1L);
			account.getBalance().setCents(150 * 100L);
			entityManager.persist(account);
			//end::basic-hbm-convert-money-converter-mutability-plan-example[]
		});
	}

}
