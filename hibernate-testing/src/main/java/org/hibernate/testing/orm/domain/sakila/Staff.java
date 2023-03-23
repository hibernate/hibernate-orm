/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.sakila;

import java.time.LocalDateTime;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author Nathan Xu
 */
@Entity
@Table( name = "staff" )
public class Staff {
	private Integer id;
	private String firstName;
	private String lastName;
	private Address address;
	private byte[] picture;
	private String email;
	private Store store;
	private Boolean active;
	private String username;
	private String password;
	private LocalDateTime lastUpdate;

	public Staff() {
	}

	public Staff(
			Integer id,
			String firstName,
			String lastName,
			Address address,
			byte[] picture,
			String email,
			Store store, Boolean active, String username, String password) {
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
		this.address = address;
		this.picture = picture;
		this.email = email;
		this.store = store;
		this.active = active;
		this.username = username;
		this.password = password;
	}

	@Id
	@Column( name = "staff_id" )
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column( name = "first_name", nullable = false, length = 45 )
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	@Column( name = "last_name", nullable = false, length = 45 )
	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@ManyToOne( optional = false )
	@JoinColumn( name = "address_id" )
	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	@Lob
	@Basic( fetch = FetchType.LAZY )
	public byte[] getPicture() {
		return picture;
	}

	public void setPicture(byte[] picture) {
		this.picture = picture;
	}

	@Column( length = 50 )
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@ManyToOne( optional = false )
	@JoinColumn( name = "store_id" )
	public Store getStore() {
		return store;
	}

	public void setStore(Store store) {
		this.store = store;
	}

	@Column( nullable = false )
	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	@Column( nullable = false, length = 16 )
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@Column( length = 40 )
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Column( name = "last_update", nullable = false )
	public LocalDateTime getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(LocalDateTime lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
}
