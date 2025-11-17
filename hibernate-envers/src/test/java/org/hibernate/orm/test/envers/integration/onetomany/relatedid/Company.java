/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.relatedid;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class Company {
	@Id
	@GeneratedValue
	private Integer id;
	private String name;
	@OneToMany
	private List<Employee> employees = new ArrayList<Employee>();

	Company() {

	}

	public Company(String name) {
		this.name = name;
	}

	public Company(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Employee> getEmployees() {
		return employees;
	}

	public void setEmployees(List<Employee> employees) {
		this.employees = employees;
	}

	@Override
	public int hashCode() {
		int result;
		result = ( id != null ? id.hashCode() : 0 );
		result = 31 * result + ( name != null ? name.hashCode() : 0 );
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if ( object == this ) {
			return true;
		}
		if ( !( object instanceof Company) ) {
			return false;
		}
		Company that = (Company) object;
		if ( getId() != null ? !getId().equals( that.getId() ) : that.getId() != null ) {
			return false;
		}
		if ( getName() != null ? !getName().equals( that.getName() ) : that.getName() != null ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Company{" +
				"id=" + id +
				", name='" + name + '\'' + '}';
	}
}
