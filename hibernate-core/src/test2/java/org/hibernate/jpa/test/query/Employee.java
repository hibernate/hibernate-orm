/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.query;

import java.io.Serializable;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "Employee")
public class Employee implements Serializable {
	@Id
	@GeneratedValue
	private Long id;

	private String name;

	private Double salary;

	public Employee() {
	}

	public Employee(String name, Double salary) {
		this.name = name;
		this.salary = salary;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Employee ) ) return false;

		Employee employee = (Employee) o;

		if ( id != null ? !id.equals( employee.id ) : employee.id != null ) return false;
		if ( name != null ? !name.equals( employee.name ) : employee.name != null ) return false;
		if ( salary != null ? !salary.equals( employee.salary ) : employee.salary != null ) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (salary != null ? salary.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Employee(id = " + id + ", name = " + name + ", salary = " + salary + ")";
	}

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

	public Double getSalary() {
		return salary;
	}

	public void setSalary(Double salary) {
		this.salary = salary;
	}
}
