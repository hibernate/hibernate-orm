package org.hibernate.query.criteria.internal.hhh14197;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

/**
 * @author Archie Cobbs
 */

@Entity
public class Department extends AbstractPersistent {

	private String name;
	private Set<Employee> employees = new HashSet<>();

	public String getName() {
		return this.name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@OneToMany(mappedBy = "department")
	public Set<Employee> getEmployees() {
		return this.employees;
	}
	public void setEmployees(Set<Employee> employees) {
		this.employees = employees;
	}
}
