package org.hibernate.processor.test.compositevalue;

import java.math.BigDecimal;

// deliberately not annotated @Embeddable
public class Money implements Comparable<Money> {
	BigDecimal amount;
	String currency;

	@Override
	public int compareTo(Money that) {
		return amount.compareTo( that.amount );
	}
}
