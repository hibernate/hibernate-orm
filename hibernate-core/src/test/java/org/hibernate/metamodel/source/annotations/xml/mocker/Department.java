package org.hibernate.metamodel.source.annotations.xml.mocker;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * @author Strong Liu
 */
@Entity
@Access(AccessType.PROPERTY)
public class Department {
	@Id
	@Access(AccessType.FIELD)
	Long id;

	@ManyToOne
	@Access(AccessType.FIELD)
	Set<Employee> employees = new HashSet<Employee>();
	String description;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Set<Employee> getEmployees() {
		return employees;
	}

	public void setEmployees(Set<Employee> employees) {
		this.employees = employees;
	}

}
