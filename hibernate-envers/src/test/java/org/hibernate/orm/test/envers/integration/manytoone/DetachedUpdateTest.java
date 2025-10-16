/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytoone;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test updating a detached audited entity using native Session API
 * with a many-to-one association.
 *
 * @author Chris Cranford
 */
@JiraKey("HHH-11859")
@EnversTest
@Jpa(annotatedClasses = {DetachedUpdateTest.Bank.class, DetachedUpdateTest.BankContact.class})
public class DetachedUpdateTest {
	private Bank bank1;
	private Bank bank2;
	private BankContact contact;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			bank1 = new Bank();
			bank1.setDescription( "Bank of Italy" );
			em.persist( bank1 );

			bank2 = new Bank();
			bank2.setDescription( "Bradesco Bank" );
			em.persist( bank2 );

			contact = new BankContact();
			contact.setBank( bank1 );
			contact.setPhoneNumber( "1234" );
			contact.setName( "Test" );
			em.persist( contact );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			contact.setName( "Other" );
			contact.setBank( bank2 );
			em.merge( contact );
		} );

		// Revision 3
		// Test changing the detached entity reference to Bank and delete the prior reference
		// within the same transaction to make sure the audit history flushes properly.
		scope.inTransaction( em -> {
			contact.setBank( bank1 );
			em.remove( em.merge( bank2 ) );
			em.merge( contact );
		} );
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Collections.singletonList( 1 ), auditReader.getRevisions( Bank.class, bank1.getId() ) );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( Bank.class, bank2.getId() ) );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( BankContact.class, contact.getId() ) );
		} );
	}

	@Test
	public void testRevisionHistory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final BankContact rev1 = auditReader.find( BankContact.class, contact.getId(), 1 );
			assertEquals( rev1.getBank(), bank1 );

			final BankContact rev2 = auditReader.find( BankContact.class, contact.getId(), 2 );
			assertEquals( rev2.getBank(), bank2 );

			final BankContact rev3 = auditReader.find( BankContact.class, contact.getId(), 3 );
			assertEquals( rev3.getBank(), bank1 );
		} );
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
