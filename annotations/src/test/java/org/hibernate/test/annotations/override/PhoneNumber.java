package org.hibernate.test.annotations.override;

import org.hibernate.test.annotations.override.Employee;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.Collection;

@Entity
public class PhoneNumber {

	@Id
	int id;

	public void setId(int id) {
		this.id = id;
	}

	int number;

	@ManyToMany(mappedBy = "contactInfo.phoneNumbers", cascade = CascadeType.ALL)
	Collection<Employee> employees;

	public Collection<Employee> getEmployees() {
		return employees;
	}

	public void setEmployees(Collection<Employee> employees) {
		this.employees = employees;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public int getId() {
		return id;
	}
}
