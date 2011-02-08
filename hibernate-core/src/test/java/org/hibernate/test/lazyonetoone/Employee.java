//$Id: Employee.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.lazyonetoone;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Gavin King
 */
public class Employee {
	private String personName;
	private Person person;
	private Collection employments = new ArrayList(); 
	Employee() {}
	public Employee(Person p) {
		this.person = p;
		this.personName = p.getName();
		p.setEmployee(this);
	}
	public Person getPerson() {
		return person;
	}
	public void setPerson(Person person) {
		this.person = person;
	}
	public String getPersonName() {
		return personName;
	}
	public void setPersonName(String personName) {
		this.personName = personName;
	}
	public Collection getEmployments() {
		return employments;
	}
	public void setEmployments(Collection employments) {
		this.employments = employments;
	}
}
