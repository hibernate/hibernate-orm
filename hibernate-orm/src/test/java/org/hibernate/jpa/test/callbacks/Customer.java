/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.jpa.test.callbacks;
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
@Table(name = "CUSTOMER_TABLE")
@Inheritance(strategy = InheritanceType.JOINED)
public class Customer extends Person {
	private Employee salesperson;
	private String comments;

	@OneToOne
	@JoinColumn(name = "salesperson")
	public Employee getSalesperson() {
		return salesperson;
	}

	public void setSalesperson(Employee salesperson) {
		this.salesperson = salesperson;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}
}
