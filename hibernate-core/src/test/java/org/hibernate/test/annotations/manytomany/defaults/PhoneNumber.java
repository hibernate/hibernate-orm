package org.hibernate.test.annotations.manytomany.defaults;
import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.hibernate.test.annotations.manytomany.Employee;

@Entity
public class PhoneNumber {
	int phNumber;
	Collection<org.hibernate.test.annotations.manytomany.Employee> employees;

	@Id
	public int getPhNumber() {
		return phNumber;
	}

	public void setPhNumber(int phNumber) {
		this.phNumber = phNumber;
	}

	@ManyToMany(mappedBy="contactInfo.phoneNumbers", cascade= CascadeType.ALL)
	public Collection<org.hibernate.test.annotations.manytomany.Employee> getEmployees() {
		return employees;
	}

	public void setEmployees(Collection<Employee> employees) {
		this.employees = employees;
	}
}
