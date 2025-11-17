/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.join;

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
