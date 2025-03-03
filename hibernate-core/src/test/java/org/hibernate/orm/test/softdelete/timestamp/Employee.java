/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.timestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Bag;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "employees")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class Employee {
	@Id
	private Integer id;
	@Basic
	private String name;

	@ManyToOne
	@JoinColumn(name = "manager_fk")
	private Employee manager;

	@OneToMany(mappedBy = "manager")
	private Set<Employee> minions;

	@Bag
	@ElementCollection
	@CollectionTable( name = "employee_accolades", joinColumns = @JoinColumn( name = "employee_fk" ) )
	@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_on")
	private List<String> accolades;

	protected Employee() {
		// for Hibernate use
	}

	public Employee(Integer id, String name, Employee manager) {
		this.id = id;
		this.name = name;
		this.manager = manager;

		this.minions = new HashSet<>();
		this.accolades = new ArrayList<>();

		if ( manager != null ) {
			manager.minions.add( this );
		}
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Employee getManager() {
		return manager;
	}

	public void setManager(Employee manager) {
		this.manager = manager;
	}

	public Set<Employee> getMinions() {
		return minions;
	}

	public void setMinions(Set<Employee> minions) {
		this.minions = minions;
	}

	public List<String> getAccolades() {
		return accolades;
	}

	public void setAccolades(List<String> accolades) {
		this.accolades = accolades;
	}
}
