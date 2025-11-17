/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.join;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SecondaryTable;

/**
 * @author Gavin King
 * @author Steve Ebersole
 */
@Entity
@DiscriminatorValue( "E" )
@SecondaryTable( name = "employee" )
public class Employee extends Person {
	private String title;
	private BigDecimal salary;
	private Employee manager;

	@Column( table = "employee", name = "`title`")
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@ManyToOne
	@JoinColumn( table = "employee" )
	public Employee getManager() {
		return manager;
	}

	public void setManager(Employee manager) {
		this.manager = manager;
	}

	@Column( table = "employee" )
	public BigDecimal getSalary() {
		return salary;
	}

	public void setSalary(BigDecimal salary) {
		this.salary = salary;
	}
}
