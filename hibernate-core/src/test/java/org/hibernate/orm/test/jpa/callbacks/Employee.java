/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks;
import java.math.BigDecimal;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "EMPLOYEE_TABLE")
@Inheritance(strategy = InheritanceType.JOINED)
public class Employee extends Person {
	private String title;
	private BigDecimal salary;
	private Employee manager;

	/**
	 * @return Returns the title.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title The title to set.
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	@OneToOne
	@JoinColumn(name = "manager")
	public Employee getManager() {
		return manager;
	}

	/**
	 * @param manager The manager to set.
	 */
	public void setManager(Employee manager) {
		this.manager = manager;
	}

	/**
	 * @return Returns the salary.
	 */
	public BigDecimal getSalary() {
		return salary;
	}

	/**
	 * @param salary The salary to set.
	 */
	public void setSalary(BigDecimal salary) {
		this.salary = salary;
	}
}
