/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.converter.hbm;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class MoneyConverterHbmTest extends BaseEntityManagerFunctionalTestCase {

	@Test
	public void testConverterMutability() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			Account account = new Account();
			account.setId( 1L );
			account.setOwner( "John Doe" );
			account.setBalance( new Money( 250 * 100L ) );

			entityManager.persist( account );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::basic-hbm-convert-money-converter-mutability-plan-example[]
			Account account = entityManager.find( Account.class, 1L );
			account.getBalance().setCents( 150 * 100L );
			entityManager.persist( account );
			//end::basic-hbm-convert-money-converter-mutability-plan-example[]
		} );
	}

	@Override
	protected String[] getMappings() {
		return new String[] {
				"org/hibernate/userguide/mapping/converter/hbm/MoneyConverterHbmTest.hbm.xml"
		};
	}
}
