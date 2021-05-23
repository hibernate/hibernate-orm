/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.sakila;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author Nathan Xu
 */
@Entity
@Table( name = "store" )
public class Store {
	private Integer id;
	private Staff managerStuff;
	private Address address;
	private LocalDateTime lastUpdate;

	public Store() {
	}

	public Store(Integer id, Staff managerStuff, Address address) {
		this.id = id;
		this.managerStuff = managerStuff;
		this.address = address;
	}

	@Id
	@Column( name = "store_id" )
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ManyToOne( optional = false )
	@JoinColumn( name = "manager_stuff_id" )
	public Staff getManagerStuff() {
		return managerStuff;
	}

	public void setManagerStuff(Staff managerStuff) {
		this.managerStuff = managerStuff;
	}

	@ManyToOne( optional = false )
	@JoinColumn( name = "address_id" )
	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	@Column( name = "last_update", nullable = false )
	public LocalDateTime getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(LocalDateTime lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
}
