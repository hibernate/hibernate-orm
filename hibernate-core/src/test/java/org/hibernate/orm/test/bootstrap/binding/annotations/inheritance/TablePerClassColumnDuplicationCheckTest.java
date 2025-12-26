/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.inheritance;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;
import org.hibernate.annotations.SecondaryRow;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;

@DomainModel(
		annotatedClasses = {TablePerClassColumnDuplicationCheckTest.Pool.class, TablePerClassColumnDuplicationCheckTest.SwimmingPool.class})
@Jira("https://hibernate.atlassian.net/browse/HHH-12678")
public class TablePerClassColumnDuplicationCheckTest {

	@Test
	public void test(DomainModelScope scope) {
		scope.getDomainModel().validate();
	}

	@Entity
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static class Pool {
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
	public static class PoolAddress {
		@Column(table = "POOL_ADDRESS")
		private String address;

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}
	}

	@Entity
	@SecondaryTables({
			@SecondaryTable(name = "POOL_ADDRESS"),
			@SecondaryTable(name = "POOL_ADDRESS_2")
	})
	@SecondaryRow(table = "POOL_ADDRESS", optional = true)
	@SecondaryRow(table = "POOL_ADDRESS_2", optional = true, owned = false)
	public static class SwimmingPool extends Pool {

		@Embedded
		private PoolAddress address;

		@Embedded
		@AttributeOverride(name = "address", column = @Column(table = "POOL_ADDRESS_2"))
		private PoolAddress secondaryAddress;

		public PoolAddress getAddress() {
			return address;
		}

		public void setAddress(PoolAddress address) {
			this.address = address;
		}

		public PoolAddress getSecondaryAddress() {
			return secondaryAddress;
		}

		public void setSecondaryAddress(PoolAddress secondaryAddress) {
			this.secondaryAddress = secondaryAddress;
		}

	}
}
