/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.usertypeaggregates.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;

public class Decimal extends Number implements Serializable, Comparable<Decimal> {

	public static final int ACCOUNT_ROUNDING_SCALE = 2;
	public static final int CURRENCY_RATE_ROUNDING_SCALE = 4;

	public static final int QUOTA_ROUNDING_SCALE = 2;
	public static final Decimal ZERO = new Decimal(BigDecimal.ZERO);

	public static final Decimal ONE = new Decimal(BigDecimal.ONE);

	public static final Decimal MINUS_ONE = new Decimal("-1");

	public static final Decimal TEN = new Decimal(BigDecimal.TEN);

	public static final Decimal HUNDRED = new Decimal("100");

	public static final Decimal THOUSAND = new Decimal("1000");

	public static final char DEFAULT_DECIMAL_SEPARATOR = ',';

	private static final char ZERO_CHAR = '0';

	private final BigDecimal bigDecimal;

	private Decimal() {
		this(BigDecimal.ZERO);
	}

	public Decimal(final String val) {
		this(new BigDecimal(val));
	}

	public Decimal(final BigDecimal bigDecimal) {
		this.bigDecimal = bigDecimal;
	}

	public Decimal(final String val, final MathContext mc) {
		this(new BigDecimal(val, mc));
	}

	public Decimal(final double val) {
		this(new BigDecimal(val));
	}

	public Decimal(final double val, final MathContext mc) {
		this(new BigDecimal(val, mc));
	}

	public Decimal(final int val) {
		this(new BigDecimal(val));
	}

	public Decimal(final int val, final MathContext mc) {
		this(new BigDecimal(val, mc));
	}

	public static Decimal from(final String val) {
		return new Decimal(val);
	}

	public static Decimal from(final double val) {
		return new Decimal(val);
	}

	public static Decimal from(final int val) {
		return new Decimal(val);
	}

	public BigDecimal getBigDecimal() {
		return this.bigDecimal;
	}

	public boolean getBooleanValue() {
		if (BigDecimal.ONE.equals(this.bigDecimal)) {
			return true;
		}
		if (BigDecimal.ZERO.equals(this.bigDecimal)) {
			return false;
		}
		throw new IllegalArgumentException("No boolean representation for value: " + this.bigDecimal);
	}

	public float getFloatValue() {
		return this.bigDecimal.floatValue();
	}

	public Decimal add(final Decimal decimal) {
		BigDecimal b = decimal.getBigDecimal();
		BigDecimal sum = this.bigDecimal.add(b);
		return new Decimal(sum);
	}

	public Decimal add(final String val) {
		return add(new Decimal(val));
	}

	public Decimal subtract(final Decimal decimal) {
		BigDecimal b = decimal.getBigDecimal();
		BigDecimal sub = this.bigDecimal.subtract(b);
		return new Decimal(sub);
	}

	public Decimal subtract(final String val) {
		return subtract(new Decimal(val));
	}

	public Decimal multiply(final Decimal decimal) {
		BigDecimal b = decimal.getBigDecimal();
		BigDecimal multiply = this.bigDecimal.multiply(b);
		return new Decimal(multiply);
	}

	public Decimal multiplyAcc(final Decimal decimal) {
		return multiply(decimal, ACCOUNT_ROUNDING_SCALE);
	}

	public Decimal multiply(final Decimal decimal, final int scale) {
		BigDecimal b = decimal.getBigDecimal();
		BigDecimal multiply = this.bigDecimal.multiply(b);

		return new Decimal(multiply).round(scale);
	}

	public Decimal divideAcc(final Decimal decimal) {
		return divide(decimal, ACCOUNT_ROUNDING_SCALE);
	}

	public Decimal divide(final Decimal decimal, final int scale) {
		return divide(decimal, scale, RoundingMode.HALF_UP);
	}

	public Decimal divide(final Decimal decimal, final int scale, final RoundingMode roundingMode) {
		BigDecimal b = decimal.getBigDecimal();
		BigDecimal divide = this.bigDecimal.divide(b, scale, roundingMode);
		return new Decimal(divide);
	}

	public Decimal abs() {
		BigDecimal abs = this.bigDecimal.abs();
		return new Decimal(abs);
	}

	@Override
	public int compareTo(final Decimal decimal) {
		BigDecimal b = decimal.getBigDecimal();
		return this.bigDecimal.compareTo(b);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj instanceof Decimal) {
			return isEqual((Decimal) obj);
		}
		return false;
	}

	public boolean isGreater(final Decimal decimal) {
		return compareTo(decimal) > 0;
	}

	public boolean isGreater(final String val) {
		return compareTo(new Decimal(val)) > 0;
	}

	public boolean isGreaterOrEqual(final Decimal decimal) {
		return compareTo(decimal) >= 0;
	}

	public boolean isGreaterOrEqual(final String val) {
		return compareTo(new Decimal(val)) >= 0;
	}

	public boolean isEqual(final Decimal decimal) {
		return compareTo(decimal) == 0;
	}

	public boolean isEqual(final String val) {
		return compareTo(new Decimal(val)) == 0;
	}

	public boolean isLess(final Decimal decimal) {
		return compareTo(decimal) < 0;
	}

	public boolean isLess(final String val) {
		return compareTo(new Decimal(val)) < 0;
	}

	public boolean isLessOrEqual(final Decimal decimal) {
		return compareTo(decimal) <= 0;
	}

	public boolean isLessOrEqual(final String val) {
		return compareTo(new Decimal(val)) <= 0;
	}

	public int signum() {
		return this.bigDecimal.signum();
	}

	public boolean isZero() {
		return signum() == 0;
	}

	public boolean isNegative() {
		return signum() == -1;
	}

	public boolean isPositive() {
		return signum() == 1;
	}

	public Decimal max(final Decimal decimal) {
		return compareTo(decimal) >= 0 ? this : decimal;
	}

	public Decimal min(final Decimal decimal) {
		return compareTo(decimal) <= 0 ? this : decimal;
	}

	@Override
	public int hashCode() {
		String number = this.toString();

		char[] charArray = number.toCharArray();

		StringBuilder sb = new StringBuilder();
		boolean separatorOccurs = false;

		for (int i = charArray.length - 1; i >= 0; i--) {
			char charAt = charArray[i];
			if ((ZERO_CHAR == charAt) && (!separatorOccurs)) {
				continue;
			}
			if (DEFAULT_DECIMAL_SEPARATOR == charAt) {
				separatorOccurs = true;
				continue;
			}
			sb.insert(0, charAt);
		}

		return sb.toString().hashCode();
	}

	public Decimal round(final int scale) {
		BigDecimal round = this.bigDecimal.setScale(scale, RoundingMode.HALF_UP);
		return new Decimal(round);
	}

	public Decimal round(final int scale, final RoundingMode roundingMode) {
		BigDecimal round = this.bigDecimal.setScale(scale, roundingMode);
		return new Decimal(round);
	}

	public Decimal roundQuota() {
		BigDecimal round = this.bigDecimal.setScale(QUOTA_ROUNDING_SCALE, RoundingMode.HALF_UP);
		return new Decimal(round);
	}

	public int getScale() {
		return this.bigDecimal.scale();
	}

	public Decimal negate() {
		return new Decimal(this.bigDecimal.negate());
	}

	public Decimal scaleByPowerOfTen(final int n) {
		return new Decimal(this.bigDecimal.scaleByPowerOfTen(n));
	}

	public String toFormatedString(final NumberFormat numberFormat) {
		return numberFormat.format(this.bigDecimal);
	}

	@Override
	public String toString() {
		return toPlainString();
	}

	/**
	 * @see BigDecimal.toPlainString()
	 * @return
	 */
	public String toPlainString() {
		return this.bigDecimal.toPlainString();
	}

	@Override
	public double doubleValue() {
		return this.bigDecimal.doubleValue();
	}

	@Override
	public float floatValue() {
		return this.bigDecimal.floatValue();
	}

	@Override
	public int intValue() {
		return this.bigDecimal.intValue();
	}

	@Override
	public long longValue() {
		return this.bigDecimal.longValue();
	}

	public Decimal trim(final int scale) {
		BigDecimal round = this.bigDecimal.setScale(scale, RoundingMode.DOWN);
		return new Decimal(round);
	}

	public Decimal stripTrailingZeros() {

		if (Decimal.ZERO.isEqual(this)) {
			return Decimal.ZERO;
		} else {
			return new Decimal(this.bigDecimal.stripTrailingZeros());
		}
	}

	public Decimal fractionalPart() {
		return new Decimal(this.bigDecimal.remainder(BigDecimal.ONE));
	}

	// ############## FOR GROOVY OPERATORS OVERRIDE

	public Decimal plus(final Decimal add) {
		return this.add(add);
	}

	public Decimal plus(final Integer add) {
		return this.add(new Decimal(add));
	}

	public Decimal plus(final Long add) {
		return this.add(new Decimal(add));
	}

	public Decimal plus(final BigDecimal add) {
		return this.add(new Decimal(add));
	}

	public Decimal minus(final Decimal add) {
		return this.subtract(add);
	}

	public Decimal minus(final Integer add) {
		return this.subtract(new Decimal(add));
	}

	public Decimal minus(final Long add) {
		return this.subtract(new Decimal(add));
	}

	public Decimal minus(final BigDecimal add) {
		return this.subtract(new Decimal(add));
	}

	public Decimal multiply(final Integer add, final int scale) {
		return this.multiply(new Decimal(add), scale);
	}

	public Decimal multiply(final Long add, final int scale) {
		return this.multiply(new Decimal(add), scale);
	}

	public Decimal multiply(final BigDecimal add, final int scale) {
		return this.multiply(new Decimal(add), scale);
	}

	public Decimal divide(final Integer add, final int scale) {
		return this.divide(new Decimal(add), scale);
	}

	public Decimal divide(final Long add, final int scale) {
		return this.divide(new Decimal(add), scale);
	}

	public Decimal divide(final BigDecimal add, final int scale) {
		return this.divide(new Decimal(add), scale);
	}

	public Decimal divide(final Integer add, final int scale, final RoundingMode roundingMode) {
		return this.divide(new Decimal(add), scale, roundingMode);
	}

	public Decimal divide(final Long add, final int scale, final RoundingMode roundingMode) {
		return this.divide(new Decimal(add), scale, roundingMode);
	}

	public Decimal divide(final BigDecimal add, final int scale, final RoundingMode roundingMode) {
		return this.divide(new Decimal(add), scale, roundingMode);
	}
}
