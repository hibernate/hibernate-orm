/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.manytoone;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.mapping.Table;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11463")
public class EmbeddedIdManyToOneForeignKeyTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Customer.class, CustomerAddress.class, Address.class };
	}

	@Test
	public void testJoinTableForeignKeyToNonAuditTables() {
		// there should only be references to REVINFO and not to the Customer or Address tables
		for ( Table table : metadata().getDatabase().getDefaultNamespace().getTables() ) {
			if ( table.getName().equals( "CustomerAddress_AUD" ) ) {
				for ( org.hibernate.mapping.ForeignKey foreignKey : table.getForeignKeys().values() ) {
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
