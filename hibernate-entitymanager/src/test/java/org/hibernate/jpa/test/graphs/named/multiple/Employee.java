package org.hibernate.jpa.test.graphs.named.multiple;

import javax.persistence.*;

/**
 * Employee
 *
 * @author Scott Marlow
 */
@Entity(name = "Employee")
@NamedEntityGraphs({
		@NamedEntityGraph(
				name = "name_salary_graph",
				includeAllAttributes = false,
				attributeNodes = {
						@NamedAttributeNode(value = "name"),
						@NamedAttributeNode(value = "salary")
				}
		),
})
public class Employee {
	@Id
	public Long id;

	private String name;
	private double salary;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getSalary() {
		return salary;
	}

	public void setSalary(double salary) {
		this.salary = salary;
	}
}
