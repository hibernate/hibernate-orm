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
@Table( name = "city" )
public class City {
	private Integer id;
	private String city;
	private Country country;
	private LocalDateTime lastUpdate;

	public City() {
	}

	public City(Integer id, String city, Country country) {
		this.id = id;
		this.city = city;
		this.country = country;
	}

	@Id
	@Column( name = "city_id" )
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column( nullable = false, length = 50 )
	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	@ManyToOne( optional = false )
	@JoinColumn( name = "country_id" )
	public Country getCountry() {
		return country;
	}

	public void setCountry(Country country) {
		this.country = country;
	}

	@Column( name = "last_update", nullable = false )
	public LocalDateTime getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(LocalDateTime lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
}
