/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.jpa.test.callbacks;
import java.math.BigDecimal;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "EMPLOYEE_TABLE")
@Inheritance(strategy = InheritanceType.JOINED)
public class Employee extends Person {
	private String title;
	private BigDecimal salary;
	private Employee manager;

	/**
	 * @return Returns the title.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title The title to set.
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	@OneToOne
	@JoinColumn(name = "manager")
	public Employee getManager() {
		return manager;
	}

	/**
	 * @param manager The manager to set.
	 */
	public void setManager(Employee manager) {
		this.manager = manager;
	}

	/**
	 * @return Returns the salary.
	 */
	public BigDecimal getSalary() {
		return salary;
	}

	/**
	 * @param salary The salary to set.
	 */
	public void setSalary(BigDecimal salary) {
		this.salary = salary;
	}
}

