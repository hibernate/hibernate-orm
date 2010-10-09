package org.hibernate.test.annotations.manytomany;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.Collection;

@Entity
public class PhoneNumber {
	int phNumber;
	Collection<Employee> employees;

	@Id
	public int getPhNumber() {
		return phNumber;
	}

	public void setPhNumber(int phNumber) {
		this.phNumber = phNumber;
	}

	@ManyToMany(mappedBy="contactInfo.phoneNumbers", cascade= CascadeType.ALL)
	public Collection<Employee> getEmployees() {
		return employees;
	}

	public void setEmployees(Collection<Employee> employees) {
		this.employees = employees;
	}
}
