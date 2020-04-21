/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.join;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SecondaryTable;

/**
 * @author Gavin King
 * @author Steve Ebersole
 */
@Entity
@DiscriminatorValue( "C" )
@SecondaryTable( name="customer" )
public class Customer extends Person {
	private Employee salesperson;
	private String comments;

	@ManyToOne
	@JoinColumn( table = "customer" )
	public Employee getSalesperson() {
		return salesperson;
	}

	public void setSalesperson(Employee salesperson) {
		this.salesperson = salesperson;
	}

	@Column( table = "customer" )
	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}
}
