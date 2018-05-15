/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.namingstrategy.complete;

import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.NaturalId;

/**
 * @author Steve Ebersole
 */
@Entity
public class Order {
	private Integer id;
	private String referenceCode;

	private Date placed;
	private Date fulfilled;

	private Customer customer;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Basic
	@NaturalId
	public String getReferenceCode() {
		return referenceCode;
	}

	public void setReferenceCode(String referenceCode) {
		this.referenceCode = referenceCode;
	}

	@Temporal(TemporalType.TIMESTAMP )
	public Date getPlaced() {
		return placed;
	}

	public void setPlaced(Date placed) {
		this.placed = placed;
	}

	@Temporal(TemporalType.TIMESTAMP )
	public Date getFulfilled() {
		return fulfilled;
	}

	public void setFulfilled(Date fulfilled) {
		this.fulfilled = fulfilled;
	}

	@ManyToOne
	@JoinColumn
	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}
}
