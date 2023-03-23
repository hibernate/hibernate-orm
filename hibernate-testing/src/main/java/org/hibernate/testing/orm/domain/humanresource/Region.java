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

/**
 * @author Nathan Xu
 */
@Entity
public class Region {
	private Integer id;
	private String name;

	public Region() {
	}

	public Region(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	@Id
	@Column( name = "region_id" )
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column( name = "region_name", length = 25 )
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
