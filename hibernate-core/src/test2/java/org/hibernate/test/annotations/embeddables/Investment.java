/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embeddables;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * @author Chris Pheby
 */
@Embeddable
public class Investment {
	
	private DollarValue amount;
	private String description;
    @Column(name = "`date`")
	private MyDate date;

	public DollarValue getAmount() {
		return amount;
	}

	public void setAmount(DollarValue amount) {
		this.amount = amount;
	}

	@Column(length = 500)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public MyDate getDate() {
		return date;
	}

	public void setDate(MyDate date) {
		this.date = date;
	}
}
