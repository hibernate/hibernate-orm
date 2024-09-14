/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.entity;

import java.util.Date;

import org.hibernate.annotations.EmbeddableInstantiatorRegistration;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
@EmbeddableInstantiatorRegistration( embeddableClass = MonetaryAmount.class, instantiator = MonetaryAmountInstantiator.class )
public class Ransom {
	private Integer id;
	private String kidnapperName;
	private MonetaryAmount amount;
	private Date date;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getKidnapperName() {
		return kidnapperName;
	}

	public void setKidnapperName(String kidnapperName) {
		this.kidnapperName = kidnapperName;
	}

	@Embedded
	public MonetaryAmount getAmount() {
		return amount;
	}

	public void setAmount(MonetaryAmount amount) {
		this.amount = amount;
	}
	@Column(name="ransom_date")
	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
}
