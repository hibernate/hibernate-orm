//$Id: Employee.java 6673 2005-05-03 22:59:24Z epbernard $
package org.hibernate.test.unionsubclass;

/**
 * @author Emmanuel Bernard
 */
public class Employee extends Human {
	private Double salary;

	public Double getSalary() {
		return salary;
	}

	public void setSalary(Double salary) {
		this.salary = salary;
	}
}
