/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ternary;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Employee {
	private String name;
	private Date hireDate;
	private Map<Site,Employee> managerBySite = new HashMap<>();
	private Set<Employee> underlings = new HashSet<>();

	Employee() {}
	public Employee(String name) {
		this.name=name;
	}
	public Map<Site,Employee> getManagerBySite() {
		return managerBySite;
	}
	public void setManagerBySite(Map<Site,Employee> managerBySite) {
		this.managerBySite = managerBySite;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Set<Employee> getUnderlings() {
		return underlings;
	}
	public void setUnderlings(Set<Employee> underlings) {
		this.underlings = underlings;
	}
	public Date getHireDate() {
		return hireDate;
	}
	public void setHireDate(Date hireDate) {
		this.hireDate = hireDate;
	}
}
