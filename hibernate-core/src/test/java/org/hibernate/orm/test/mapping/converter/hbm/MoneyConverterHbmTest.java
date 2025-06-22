/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converter.hbm;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class MoneyConverterHbmTest extends BaseEntityManagerFunctionalTestCase {

	@Test
	public void testConverterMutability() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			Account account = new Account();
			account.setId(1L);
			account.setOwner("John Doe");
			account.setBalance(new Money(250 * 100L));

			entityManager.persist(account);
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::basic-hbm-convert-money-converter-mutability-plan-example[]
			Account account = entityManager.find(Account.class, 1L);
			account.getBalance().setCents(150 * 100L);
			entityManager.persist(account);
			//end::basic-hbm-convert-money-converter-mutability-plan-example[]
		});
	}

	@Override
	protected String[] getMappings() {
		return new String[] {
				"org/hibernate/orm/test/mapping/converter/hbm/MoneyConverterHbmTest.hbm.xml"
		};
	}
}
