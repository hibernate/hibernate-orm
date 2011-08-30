//$Id: Person.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.lazyonetoone;


/**
 * @author Gavin King
 */
public class Person {
	private String name;
	private Employee employee;
	Person() {}
	public Person(String name) {
		this.name = name;
	}
	public Employee getEmployee() {
		return employee;
	}
	public void setEmployee(Employee employee) {
		this.employee = employee;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
