/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetomany;

import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.PrimaryKeyJoinColumns;

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
