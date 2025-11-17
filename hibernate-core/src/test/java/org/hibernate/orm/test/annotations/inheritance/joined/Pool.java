/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance.joined;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;

import org.hibernate.annotations.SecondaryRow;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@SecondaryTables({
	@SecondaryTable(name="POOL_ADDRESS"),
	@SecondaryTable(name="POOL_ADDRESS_2")
})
@SecondaryRow(table ="POOL_ADDRESS", optional=true)
@SecondaryRow(table ="POOL_ADDRESS_2", optional=true, owned = false)
public class Pool {
	@Id @GeneratedValue
	private Integer id;

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

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
