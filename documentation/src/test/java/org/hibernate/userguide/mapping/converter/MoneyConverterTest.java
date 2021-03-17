/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class MoneyConverterTest extends BaseEntityManagerFunctionalTestCase {

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
			//tag::basic-jpa-convert-money-converter-mutability-plan-example[]
			Account account = entityManager.find( Account.class, 1L );
			account.getBalance().setCents( 150 * 100L );
			entityManager.persist( account );
			//end::basic-jpa-convert-money-converter-mutability-plan-example[]
		} );
	}


	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Account.class };
	}

	//tag::basic-jpa-convert-money-converter-mapping-example[]
	public static class Money {

		private long cents;

		//Getters and setters are omitted for brevity
	//end::basic-jpa-convert-money-converter-mapping-example[]

		public Money(long cents) {
			this.cents = cents;
		}

		public long getCents() {
			return cents;
		}

		public void setCents(long cents) {
			this.cents = cents;
		}
	//tag::basic-jpa-convert-money-converter-mapping-example[]
	}
	//end::basic-jpa-convert-money-converter-mapping-example[]

	//tag::basic-jpa-convert-money-converter-mapping-example[]

	@Entity(name = "Account")
	public static class Account {

		@Id
		private Long id;

		private String owner;

		@Convert(converter = MoneyConverter.class)
		private Money balance;

		//Getters and setters are omitted for brevity
		//end::basic-jpa-convert-money-converter-mapping-example[]
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getOwner() {
			return owner;
		}

		public void setOwner(String owner) {
			this.owner = owner;
		}

		public Money getBalance() {
			return balance;
		}

		public void setBalance(Money balance) {
			this.balance = balance;
		}
	//tag::basic-jpa-convert-money-converter-mapping-example[]
	}

	public static class MoneyConverter
			implements AttributeConverter<Money, Long> {

		@Override
		public Long convertToDatabaseColumn(Money attribute) {
			return attribute == null ? null : attribute.getCents();
		}

		@Override
		public Money convertToEntityAttribute(Long dbData) {
			return dbData == null ? null : new Money( dbData );
		}
	}
	//end::basic-jpa-convert-money-converter-mapping-example[]
}
