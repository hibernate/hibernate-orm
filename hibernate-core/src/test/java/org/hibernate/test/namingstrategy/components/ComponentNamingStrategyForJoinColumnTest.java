/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */


import java.util.ArrayList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Cai Chun
 */
@TestForIssue(jiraKey = "HHH-11826")
public class ComponentNamingStrategyForJoinColumnTest extends BaseUnitTestCase {
	@Test
	public void testNamingComponentPath() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();

		try {
			final MetadataSources ms = new MetadataSources( ssr );
			ms.addAnnotatedClass( Employee.class ).addAnnotatedClass( BankAccounts.class ).addAnnotatedClass( BankAccount
																													  .class ).addAnnotatedClass( WebUser.class );

			final Metadata metadata = ms.getMetadataBuilder()
					.applyImplicitNamingStrategy( ImplicitNamingStrategyComponentPathImpl.INSTANCE )
					.build();

			checkDefaultJoinTableAndAllColumnNames(
					metadata,
					Employee.class,
					"bankAccounts.accounts",
					"ComponentNamingStrategyForJoinColumnTest$Employee_bankAccounts_accounts",
					"ComponentNamingStrategyForJoinColumnTest$Employee_id",
					new String[]{"ComponentNamingStrategyForJoinColumnTest$Employee_id",
							"bankAccounts_accounts_accountNumber",
							"bankAccounts_accounts_bankName",
							"bankAccounts_accounts_verificationUser_id"}
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
		assertEquals ( 1, collection.getOwner().getKey().getColumnSpan() );
		assertEquals( ownerForeignKeyNameExpected, collection.getKey().getColumnIterator().next().getText() );

		int columnNumber = table.getColumnSpan();
		for ( int i = 0; i < columnNumber; i++ ) {
			assertEquals( columnNames[i], table.getColumn(i+1).getName());
		}
	}

	@Entity
	public static class Employee {

		private long id;
		private String name;
		private BankAccounts bankAccounts;

		@Id
		@GeneratedValue(
				strategy = GenerationType.AUTO
		)
		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		@Basic
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Embedded
		public BankAccounts getBankAccounts() {
			if ( bankAccounts == null ) {
				bankAccounts = new BankAccounts();
			}

			return bankAccounts;
		}

		public void setBankAccounts(BankAccounts bankAccounts) {
			this.bankAccounts = bankAccounts;
		}

	}

	@Embeddable
	public static class BankAccounts {

		private List<BankAccount> accounts;

		public BankAccounts() {
		}

		@ElementCollection(
				fetch = FetchType.LAZY
		)
		public List<BankAccount> getAccounts() {
			if( this.accounts == null ){
				this.accounts = new ArrayList<>();
			}

			return this.accounts;
		}

		public void setAccounts(List<BankAccount> bankAccounts) {
			this.accounts = bankAccounts;
		}
	}

	@Embeddable
	public static class BankAccount {

		private String bankName;
		private String accountNumber;
		private WebUser verificationUser;

		@Basic
		public String getBankName() {
			return bankName;
		}
		public void setBankName(String bankName) {
			this.bankName = bankName;
		}

		@Basic
		public String getAccountNumber() {
			return accountNumber;
		}
		public void setAccountNumber(String accountNumber) {
			this.accountNumber = accountNumber;
		}

		@ManyToOne
		public WebUser getVerificationUser() {
			return verificationUser;
		}
		public void setVerificationUser(WebUser verificationUser) {
			this.verificationUser = verificationUser;
		}
	}

	@Entity
	public static class WebUser {

		private long id;
		private String name;

		@Id
		@GeneratedValue(
				strategy = GenerationType.AUTO
		)
		public long getId() {
			return id;
		}
		public void setId(long id) {
			this.id = id;
		}

		@Basic
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
	}
}


