package org.hibernate.test.component.basic;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * todo: describe Employee
 *
 * @author Steve Ebersole
 */
public class Employee {
	private Long id;
	private Person person;
	private Date hireDate;
	private OptionalComponent optionalComponent;
	private Set directReports = new HashSet();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

	public Date getHireDate() {
		return hireDate;
	}

	public void setHireDate(Date hireDate) {
		this.hireDate = hireDate;
	}

	public OptionalComponent getOptionalComponent() {
		return optionalComponent;
	}

	public void setOptionalComponent(OptionalComponent optionalComponent) {
		this.optionalComponent = optionalComponent;
	}

	public Set getDirectReports() {
		return directReports;
	}

	public void setDirectReports(Set directReports) {
		this.directReports = directReports;
	}
}
