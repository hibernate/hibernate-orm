/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations;
import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;


/**
 * Discount ticket a client can use when buying tickets
 *
 * @author Emmanuel Bernard
 */
@Entity
public class Discount implements Serializable {

	private Long id;
	private double discount;
	private Customer owner;


	@Column(precision = 5, scale = 2)
	public double getDiscount() {
		return discount;
	}

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setDiscount(double i) {
		discount = i;
	}

	public void setId(Long long1) {
		id = long1;
	}

	@ManyToOne(cascade = {CascadeType.ALL}, fetch = FetchType.LAZY)
	@JoinColumn(name = "CUSTOMER_ID")
	public Customer getOwner() {
		return owner;
	}

	public void setOwner(Customer customer) {
		owner = customer;
	}

}

