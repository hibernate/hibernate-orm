/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;

@Entity
@PrimaryKeyJoinColumns({ @PrimaryKeyJoinColumn(name = "id_asset"), @PrimaryKeyJoinColumn(name = "id_test") })
public class Computer extends Asset {

	private String computerName;

	public Computer() {

	}

	/**
	 * @param id
	 */
	public Computer(Integer id) {
		super( id );
	}

	/**
	 * @return the computerName
	 */
	public String getComputerName() {
		return computerName;
	}

	/**
	 * @param computerName the computerName to set
	 */
	public void setComputerName(String computerName) {
		this.computerName = computerName;
	}
}
