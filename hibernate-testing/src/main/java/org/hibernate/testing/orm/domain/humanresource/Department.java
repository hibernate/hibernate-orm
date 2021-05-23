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
public class Department {
	private Integer id;
	private String name;
	private Employee manager;
	private Location location;

	public Department() {
	}

	public Department(
			Integer id,
			String name,
			Employee manager,
			Location location) {
		this.id = id;
		this.name = name;
		this.manager = manager;
		this.location = location;
	}

	@Id
	@Column( name = "department_id" )
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column( name = "department_name", nullable = false, length = 30 )
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne
	@JoinColumn( name = "manager_id" )
	public Employee getManager() {
		return manager;
	}

	public void setManager(Employee manager) {
		this.manager = manager;
	}

	@ManyToOne
	@JoinColumn( name = "location_id" )
	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}
}
