/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.humanresource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * @author Nathan Xu
 */
@Entity
public class Country {
	private String id;
	private String name;
	private Region region;

	public Country() {
	}

	public Country(String id, String name, Region region) {
		this.id = id;
		this.name = name;
		this.region = region;
	}

	@Id
	@Column( name = "country_id", length =  2 )
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Column( name = "country_name", length = 40 )
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne
	@JoinColumn( name = "region_id" )
	public Region getRegion() {
		return region;
	}

	public void setRegion(Region region) {
		this.region = region;
	}
}
