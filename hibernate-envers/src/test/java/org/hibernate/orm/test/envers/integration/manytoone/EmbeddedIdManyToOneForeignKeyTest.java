/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytoone;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.mapping.Table;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11463")
@EnversTest
@DomainModel(annotatedClasses = {
		EmbeddedIdManyToOneForeignKeyTest.Customer.class,
		EmbeddedIdManyToOneForeignKeyTest.CustomerAddress.class,
		EmbeddedIdManyToOneForeignKeyTest.Address.class
})
@SessionFactory
public class EmbeddedIdManyToOneForeignKeyTest {
	@Test
	public void testJoinTableForeignKeyToNonAuditTables(DomainModelScope scope) {
		// there should only be references to REVINFO and not to the Customer or Address tables
		for ( Table table : scope.getDomainModel().getDatabase().getDefaultNamespace().getTables() ) {
			if ( table.getName().equals( "CustomerAddress_AUD" ) ) {
				for ( var foreignKey : table.getForeignKeyCollection() ) {
					assertEquals( "REVINFO", foreignKey.getReferencedTable().getName() );
				}
			}
		}
	}

	@Audited
	@Entity(name = "Customer")
	public static class Customer {
		@Id
		private Integer id;

		@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
		@OneToMany
		@JoinTable(name = "CustomerAddress")
		@AuditJoinTable(name = "CustomerAddress_AUD")
		@JoinColumn(name = "customerId", foreignKey = @ForeignKey(name = "FK_CUSTOMER_ADDRESS"))
		private List<CustomerAddress> addresses = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<CustomerAddress> getAddresses() {
			return addresses;
		}

		public void setAddresses(List<CustomerAddress> addresses) {
			this.addresses = addresses;
		}
	}

	@Audited
	@Entity(name = "Address")
	public static class Address {
		@Id
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Embeddable
	public static class CustomerAddressId implements Serializable {
		@ManyToOne
		private Address address;
		@ManyToOne
		private Customer customer;

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}

		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}
	}

	@Audited
	@Entity(name = "CustomerAddress")
	public static class CustomerAddress {
		@EmbeddedId
		private CustomerAddressId id;
	}
}
