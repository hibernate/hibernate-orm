/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.indexcoll;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;


@Entity
public class ExchangeRate {
	public ExchangeRate() {
		super();
	}

	@Id @GeneratedValue
	private Integer id;
	
	@Column
	private double rate;
	

	
	public double getRate() {
		return rate;
	}

	public void setRate(double rate) {
		this.rate = rate;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Embedded
    private ExchangeRateKey key = new ExchangeRateKey();

	public ExchangeRateKey getKey() {
		return key;
	}

	public void setKey(ExchangeRateKey key) {
		this.key = key;
	}

	@javax.persistence.ManyToOne(fetch = FetchType.LAZY )
    private ExchangeOffice parent = null;


	public ExchangeOffice getParent() {
		return parent;
	}

	public void setParent(ExchangeOffice parent) {
		this.parent = parent;
	}
}
