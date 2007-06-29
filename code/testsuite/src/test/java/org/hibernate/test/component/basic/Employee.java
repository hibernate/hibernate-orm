package org.hibernate.test.component.basic;

import java.util.Date;

/**
 * todo: describe Employee
 *
 * @author Steve Ebersole
 */
public class Employee {
	private Long id;
	private Person person;
	private Date hireDate;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

	public Date getHireDate() {
		return hireDate;
	}

	public void setHireDate(Date hireDate) {
		this.hireDate = hireDate;
	}
}
