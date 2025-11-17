/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.indexcoll;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;


@Embeddable
public class ExchangeRateKey
{

	public ExchangeRateKey() {
		super();
	}


	public ExchangeRateKey( long date, Currency currency1, Currency currency2) {
		super();
		this.date = date;
		this.currency1 = currency1;
		this.currency2 = currency2;
	}

	@Column(name="`date`", nullable = false)
	protected long date;

	@jakarta.persistence.ManyToOne(fetch = FetchType.LAZY, optional = false)
	protected Currency currency1;

	@jakarta.persistence.ManyToOne(fetch = FetchType.LAZY, optional = false)
	protected Currency currency2;


	@Override
	public boolean equals (Object obj) {
		if (this == obj)  return true;

		if (!(obj instanceof ExchangeRateKey)) return false;

		ExchangeRateKey q = (ExchangeRateKey) obj;
		return q.date == date && q.currency1 == this.currency1 && q.currency2 == this.currency2;

	}



	@Override
	public int hashCode() {
		int hashcode = 0;
		hashcode += date;
		hashcode += (currency1 != null ? currency1.hashCode() : 0);
		hashcode += (currency2 != null ? currency2.hashCode() : 0);
		return hashcode;
	}

}
