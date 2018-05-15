/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.manytoone;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * Test updating a detached audited entity using native Session API
 * with a many-to-one association.
 *
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11859")
public class DetachedUpdateTest extends BaseEnversFunctionalTestCase {
	private Bank bank1;
	private Bank bank2;
	private BankContact contact;

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class<?>[] { Bank.class, BankContact.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		doInHibernate( this::sessionFactory, session -> {
			bank1 = new Bank();
			bank1.setDescription( "Bank of Italy" );
			session.save( bank1 );

			bank2 = new Bank();
			bank2.setDescription( "Bradesco Bank" );
			session.save( bank2 );

			contact = new BankContact();
			contact.setBank( bank1 );
			contact.setPhoneNumber( "1234" );
			contact.setName( "Test" );
			session.save( contact );
		} );

		// Revision 2
		doInHibernate( this::sessionFactory, session -> {
			contact.setName( "Other" );
			contact.setBank( bank2 );
			session.update( contact );
		} );

		// Revision 3
		// Test changing the detached entity reference to Bank and delete the prior reference
		// within the same transaction to make sure the audit history flushes properly.
		doInHibernate( this::sessionFactory, session -> {
			contact.setBank( bank1 );
			session.delete( bank2 );
			session.update( contact );
		} );
	}

	@Test
	public void testRevisionCounts() {
		assertEquals( Collections.singletonList( 1 ), getAuditReader().getRevisions( Bank.class, bank1.getId() ) );
		assertEquals( Arrays.asList( 1, 3 ), getAuditReader().getRevisions( Bank.class, bank2.getId() ) );
		assertEquals( Arrays.asList( 1, 2, 3 ), getAuditReader().getRevisions( BankContact.class, contact.getId() ) );
	}

	@Test
	public void testRevisionHistory() {
		final BankContact rev1 = getAuditReader().find( BankContact.class, contact.getId(), 1 );
		assertEquals( rev1.getBank(), bank1 );

		final BankContact rev2 = getAuditReader().find( BankContact.class, contact.getId(), 2 );
		assertEquals( rev2.getBank(), bank2 );

		final BankContact rev3 = getAuditReader().find( BankContact.class, contact.getId(), 3 );
		assertEquals( rev3.getBank(), bank1 );
	}

	@Entity(name="Bank")
	@Audited(withModifiedFlag = true)
	public static class Bank {
		@Id
		@GeneratedValue
		private Integer id;
		private String description;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Bank bank = (Bank) o;
			return Objects.equals( id, bank.id ) &&
					Objects.equals( description, bank.description );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, description );
		}
	}

	@Entity(name = "BankContact")
	@Audited(withModifiedFlag = true)
	public static class BankContact {
		@Id
		@GeneratedValue
		private Integer id;
		private String phoneNumber;
		private String name;
		@ManyToOne
		@JoinColumn(name = "bank_id")
		private Bank bank;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getPhoneNumber() {
			return phoneNumber;
		}

		public void setPhoneNumber(String phoneNumber) {
			this.phoneNumber = phoneNumber;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Bank getBank() {
			return bank;
		}

		public void setBank(Bank bank) {
			this.bank = bank;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			BankContact that = (BankContact) o;
			return Objects.equals( id, that.id ) &&
					Objects.equals( phoneNumber, that.phoneNumber ) &&
					Objects.equals( name, that.name ) &&
					Objects.equals( bank, that.bank );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, phoneNumber, name, bank );
		}
	}
}
