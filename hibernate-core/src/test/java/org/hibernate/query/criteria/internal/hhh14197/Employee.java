package org.hibernate.query.criteria.internal.hhh14197;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * @author Archie Cobbs
 */

@Entity
public class Employee extends AbstractPersistent {

	private String name;
	private float salary;
	private Seniority seniority;
	private Date startDate;
	private Department department;
	private Employee manager;
	private Set<Employee> directReports = new HashSet<>();
	private Map<String, String> annotations = new HashMap<>();

	public String getName() {
		return this.name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public float getSalary() {
		return this.salary;
	}
	public void setSalary(float salary) {
		this.salary = salary;
	}

	@Enumerated(EnumType.STRING)
	public Seniority getSeniority() {
		return this.seniority;
	}
	public void setSeniority(Seniority seniority) {
		this.seniority = seniority;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getStartDate() {
		return this.startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	@ManyToOne
	public Employee getManager() {
		return this.manager;
	}
	public void setManager(Employee manager) {
		this.manager = manager;
	}

	@ManyToOne
	public Department getDepartment() {
		return this.department;
	}
	public void setDepartment(Department department) {
		this.department = department;
	}

	@OneToMany(mappedBy = "manager")
	public Set<Employee> getDirectReports() {
		return this.directReports;
	}
	public void setDirectReports(Set<Employee> directReports) {
		this.directReports = directReports;
	}

	@ElementCollection
	@MapKeyColumn(name = "name", length = 180)
	@Column(name = "value", nullable = false)
	public Map<String, String> getAnnotations() {
		return this.annotations;
	}
	public void setAnnotations(Map<String, String> annotations) {
		this.annotations = annotations;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName()
				+ "[name=" + (this.name != null ? "\"" + this.name + "\"" : null)
				+ ",salary=" + this.salary
				+ ",startDate=" + this.startDate
				+ ",department=" + this.department
				+ ",manager=" + this.manager
				+ ",directReports=" + this.directReports
				+ ",annotations=" + this.annotations
				+ "]";
	}

// Seniority

	public enum Seniority {
		JUNIOR,
		SENIOR;
	}
}
