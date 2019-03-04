/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.manytoone;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.relational.spi.Table;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11463")
public class EmbeddedIdManyToOneForeignKeyTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Customer.class, CustomerAddress.class, Address.class };
	}

	@DynamicTest
	public void testJoinTableForeignKeyToNonAuditTables() {
		final EntityTypeDescriptor<?> entityTypeDescriptor = getMetamodel().entity( "CustomerAddress_AUD" );

		final Table primaryTable = entityTypeDescriptor.getPrimaryTable();
		for ( org.hibernate.metamodel.model.relational.spi.ForeignKey foreignKey : primaryTable.getForeignKeys() ) {
			if ( foreignKey.isExportationEnabled() ) {
				assertThat( foreignKey.getTargetTable().getTableExpression(), equalTo( "REVINFO" ) );
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
