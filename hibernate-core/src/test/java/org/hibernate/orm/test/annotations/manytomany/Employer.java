/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytomany;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import org.hibernate.annotations.Cascade;

/**
 * Employer in a employer-Employee relationship
 *
 * @author Emmanuel Bernard
 */
@Entity()
@Table(name="`Employer`")
public class Employer implements Serializable {
	private Integer id;
	private Collection<Employee> employees;
	private List<Contractor> contractors;

	@ManyToMany(
			targetEntity = Contractor.class,
			cascade = {CascadeType.PERSIST, CascadeType.MERGE}
	)
	@JoinTable(
			name = "EMPLOYER_CONTRACTOR",
			joinColumns = {@JoinColumn(name = "EMPLOYER_ID")},
			inverseJoinColumns = {@JoinColumn(name = "CONTRACTOR_ID")}
	)
	@Cascade({org.hibernate.annotations.CascadeType.PERSIST, org.hibernate.annotations.CascadeType.MERGE})
	@OrderBy("name desc")
	public List<Contractor> getContractors() {
		return contractors;
	}

	public void setContractors(List<Contractor> contractors) {
		this.contractors = contractors;
	}

	@ManyToMany(
			targetEntity = Employee.class,
			cascade = {CascadeType.PERSIST, CascadeType.MERGE}
	)
	@JoinTable(
			name = "EMPLOYER_EMPLOYEE",
			joinColumns = {@JoinColumn(name = "EMPER_ID")},
			inverseJoinColumns = {@JoinColumn(name = "EMPEE_ID")}
	)
	@Cascade({org.hibernate.annotations.CascadeType.PERSIST, org.hibernate.annotations.CascadeType.MERGE})
	@OrderBy("name asc")
	public Collection<Employee> getEmployees() {
		return employees;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setEmployees(Collection<Employee> set) {
		employees = set;
	}

	public void setId(Integer integer) {
		id = integer;
	}
}
