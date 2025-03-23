/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.generics;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				WildcardsTest.BankAccount.class,
				WildcardsTest.BalanceUsage.class
		}
)
@JiraKey(value = "HHH-15624")
public class WildcardsTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					BalanceUsage balanceUsage = new BalanceUsage( 1l, "withdrowal" );

					Set<BalanceUsage> usages = new HashSet<>();
					usages.add( balanceUsage );

					BankAccount bankAccount = new BankAccount( 2l, new BigDecimal( 1000 ), usages );
					entityManager.persist( balanceUsage );
					entityManager.persist( bankAccount );
				}
		);
	}

	@Test
	public void testIt(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					BankAccount bankAccount = entityManager.createQuery(
									"select b from BankAccount b",
									BankAccount.class
							)
							.getSingleResult();
					assertThat( bankAccount.usage.size() ).isEqualTo( 1 );
				}
		);
	}

	@Entity(name = "BankAccount")
	public static class BankAccount {

		@Id
		Long id;

		BigDecimal balance;

		@Convert(converter = BalanceUsageConverter.class)
		@Column(name = "account_usage")
		Set<? extends Usage> usage;

		public BankAccount() {
		}

		public BankAccount(Long id, BigDecimal balance, Set<? extends Usage> usage) {
			this.id = id;
			this.balance = balance;
			this.usage = usage;
		}
	}

	@Converter
	public static class BalanceUsageConverter implements AttributeConverter<Set<BalanceUsage>, String> {
		public static final String DELIMITER = ",";

		@Override
		public String convertToDatabaseColumn(Set<BalanceUsage> attribute) {
			if ( attribute.isEmpty() ) {
				return null;
			}

			final Set<String> listOfString = attribute.stream()
					.map( it -> it.toString() )
					.collect( Collectors.toSet() );

			return listOfString.stream().reduce( (s, s2) -> s + DELIMITER ).get();
		}

		@Override
		public Set<BalanceUsage> convertToEntityAttribute(String dbData) {
			return Arrays.stream( dbData.split( DELIMITER ) ).map( it -> BalanceUsage.valueOf( it ) ).collect(
					Collectors.toSet() );
		}
	}

	public interface Usage {
		String DELIMITER = ":";
	}

	@Entity(name = "BalanceUsage")
	public static class BalanceUsage implements Usage {

		@Id
		private Long id;

		private String name;

		public BalanceUsage() {
		}

		public BalanceUsage(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public static BalanceUsage valueOf(String balanceType) {
			String[] split = balanceType.split( DELIMITER );
			return new BalanceUsage( Long.getLong( split[0] ), split[1] );
		}

		@Override
		public String toString() {
			return id + DELIMITER + name;
		}
	}

}
