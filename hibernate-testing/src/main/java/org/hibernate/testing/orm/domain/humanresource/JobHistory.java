/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.humanresource;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * @author Nathan Xu
 */
@Entity
public class JobHistory {
	private EmployeeAndStartDate employeeAndStartDate;
	private LocalDate endDate;
	private Job job;
	private Department department;

	public JobHistory() {
	}

	public JobHistory(
			EmployeeAndStartDate employeeAndStartDate,
			LocalDate endDate,
			Job job,
			Department department) {
		this.employeeAndStartDate = employeeAndStartDate;
		this.endDate = endDate;
		this.job = job;
		this.department = department;
	}

	@EmbeddedId
	public EmployeeAndStartDate getEmployeeAndStartDate() {
		return employeeAndStartDate;
	}

	public void setEmployeeAndStartDate(EmployeeAndStartDate employeeAndStartDate) {
		this.employeeAndStartDate = employeeAndStartDate;
	}

	@Column( name = "end_date", nullable = false )
	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	@ManyToOne(optional = false)
	@JoinColumn( name = "job_id" )
	public Job getJob() {
		return job;
	}

	public void setJob(Job job) {
		this.job = job;
	}

	@ManyToOne
	@JoinColumn( name = "department_id" )
	public Department getDepartment() {
		return department;
	}

	public void setDepartment(Department department) {
		this.department = department;
	}

	@Embeddable
	public static class EmployeeAndStartDate implements Serializable {
		@ManyToOne
		@JoinColumn( name = "employee_id" )
		private Employee employee;

		@Column( name = "start_date" )
		private LocalDate startDate;

		public EmployeeAndStartDate() {
		}

		public EmployeeAndStartDate(Employee employee, LocalDate startDate) {
			this.employee = employee;
			this.startDate = startDate;
		}

		public Employee getEmployee() {
			return employee;
		}

		public void setEmployee(Employee employee) {
			this.employee = employee;
		}

		public LocalDate getStartDate() {
			return startDate;
		}

		public void setStartDate(LocalDate startDate) {
			this.startDate = startDate;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			EmployeeAndStartDate employeeAndStartDate = (EmployeeAndStartDate) o;
			return employee.equals( employeeAndStartDate.employee ) && startDate.equals( employeeAndStartDate.startDate );
		}

		@Override
		public int hashCode() {
			return Objects.hash( employee, startDate );
		}
	}
}
