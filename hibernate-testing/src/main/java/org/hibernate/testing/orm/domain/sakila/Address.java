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
@Table( name = "address" )
public class Address {
	private Integer id;
	private String address;
	private String address2;
	private String district;
	private City city;
	private String postCode;
	private String phone;
	private LocalDateTime lastUpdate;

	public Address() {
	}

	public Address(Integer id, String address, String address2, String district, City city, String postCode, String phone) {
		this.id = id;
		this.address = address;
		this.address2 = address2;
		this.district = district;
		this.city = city;
		this.postCode = postCode;
		this.phone = phone;
	}

	@Id
	@Column( name = "address_id" )
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column( nullable = false, length = 50 )
	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	@Column( length = 50 )
	public String getAddress2() {
		return address2;
	}

	public void setAddress2(String address2) {
		this.address2 = address2;
	}

	@Column( nullable = false, length = 20 )
	public String getDistrict() {
		return district;
	}

	public void setDistrict(String district) {
		this.district = district;
	}

	@ManyToOne
	@JoinColumn( name = "city_id" )
	public City getCity() {
		return city;
	}

	public void setCity(City city) {
		this.city = city;
	}

	@Column( name = "post_code", length = 10 )
	public String getPostCode() {
		return postCode;
	}

	public void setPostCode(String postCode) {
		this.postCode = postCode;
	}

	@Column( nullable = false, length = 20 )
	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	@Column( name = "last_update", nullable = false )
	public LocalDateTime getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(LocalDateTime lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
}
