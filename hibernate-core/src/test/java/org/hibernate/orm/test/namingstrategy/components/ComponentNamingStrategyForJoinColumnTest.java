/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.namingstrategy.components;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.metamodel.CollectionClassification;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Cai Chun
 */
@JiraKey(value = "HHH-11826")
@BaseUnitTest
public class ComponentNamingStrategyForJoinColumnTest {

	@Test
	public void testNamingComponentPath() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DEFAULT_LIST_SEMANTICS, CollectionClassification.BAG )
				.build();

		try {
			final MetadataSources ms = new MetadataSources( ssr )
				.addAnnotatedClass( Employee.class )
				.addAnnotatedClass( BankAccounts.class )
				.addAnnotatedClass( BankAccount.class )
				.addAnnotatedClass( WebUser.class );

			final Metadata metadata = ms.getMetadataBuilder()
					.applyImplicitNamingStrategy(
						ImplicitNamingStrategyComponentPathImpl.INSTANCE )
					.build();

			checkDefaultJoinTableAndAllColumnNames(
					metadata,
					Employee.class,
					"bankAccounts.accounts",
					"ComponentNamingStrategyForJoinColumnTest$Employee_bankAccounts_accounts",
					"ComponentNamingStrategyForJoinColumnTest$Employee_id",
					new String[] {
						"ComponentNamingStrategyForJoinColumnTest$Employee_id",
						"bankAccounts_accounts_accountNumber",
						"bankAccounts_accounts_bankName",
						"bankAccounts_accounts_verificationUser_id"
					}
			);
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	protected void checkDefaultJoinTableAndAllColumnNames(
			Metadata metadata,
			Class<?> ownerEntityClass,
			String ownerCollectionPropertyName,
			String expectedCollectionTableName,
			String ownerForeignKeyNameExpected,
			String[] columnNames) {
		final org.hibernate.mapping.Collection collection = metadata.getCollectionBinding( ownerEntityClass.getName() + '.' + ownerCollectionPropertyName );

		final org.hibernate.mapping.Table table = collection.getCollectionTable();
		assertEquals( expectedCollectionTableName, table.getName() );

		// The default owner and inverse join columns can only be computed if they have PK with 1 column.
		assertEquals( 1, collection.getOwner().getKey().getColumnSpan() );
		assertEquals(
			ownerForeignKeyNameExpected,
			collection.getKey().getSelectables().get( 0 ).getText()
		);

		int columnNumber = table.getColumnSpan();
		for ( int i = 0; i < columnNumber; i++ ) {
			assertEquals( columnNames[i], table.getColumn( i + 1 ).getName());
		}
	}

	@Entity
	public static class Employee {

		@Id
		@GeneratedValue(
			strategy = GenerationType.AUTO
		)
		private Long id;

		private String name;

		@Embedded
		private BankAccounts bankAccounts = new BankAccounts();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public BankAccounts getBankAccounts() {
			return bankAccounts;
		}

		public void setBankAccounts(BankAccounts bankAccounts) {
			this.bankAccounts = bankAccounts;
		}

	}

	@Embeddable
	public static class BankAccounts {

		@ElementCollection(fetch = FetchType.LAZY)
		private List<BankAccount> accounts = new ArrayList<>();

		public List<BankAccount> getAccounts() {
			return this.accounts;
		}
	}

	@Embeddable
	public static class BankAccount {

		private String bankName;

		private String accountNumber;

		@ManyToOne(fetch = FetchType.LAZY)
		private WebUser verificationUser;

		public String getBankName() {
			return bankName;
		}
		public void setBankName(String bankName) {
			this.bankName = bankName;
		}

		public String getAccountNumber() {
			return accountNumber;
		}
		public void setAccountNumber(String accountNumber) {
			this.accountNumber = accountNumber;
		}

		public WebUser getVerificationUser() {
			return verificationUser;
		}
		public void setVerificationUser(WebUser verificationUser) {
			this.verificationUser = verificationUser;
		}
	}

	@Entity
	public static class WebUser {

		@Id
		@GeneratedValue(
			strategy = GenerationType.AUTO
		)
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}


