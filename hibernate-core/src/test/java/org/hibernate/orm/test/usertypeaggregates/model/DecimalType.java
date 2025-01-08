/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.usertypeaggregates.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

/**
 * Typ dla hibernate VO
 *
 * @author Mirek Szajowski
 *
 */
public class DecimalType implements UserType<Decimal>, Serializable {

	@Override
	public Decimal assemble(final Serializable cached, final Object owner) {
		return (Decimal) cached;
	}

	@Override
	public Decimal deepCopy(final Decimal value) {
		return value;
	}

	@Override
	public Serializable disassemble(final Decimal value) throws HibernateException {
		return value;

	}

	@Override
	public boolean equals(final Decimal x, final Decimal y) throws HibernateException {
		if (x == y) {
			return true;
		}
		if (null == x || null == y) {
			return false;
		}
		return x.equals(y);
	}

	@Override
	public int hashCode(final Decimal value) throws HibernateException {
		return value.hashCode();
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Decimal replace(final Decimal original, final Decimal target, final Object owner) {
		return original;
	}

	@Override
	public void nullSafeSet(final PreparedStatement st, final Decimal value, final int index,
			final SharedSessionContractImplementor session) throws HibernateException, SQLException {
		Decimal decimal = value;
		if (decimal == null) {
			st.setNull(index, Types.NUMERIC);
		} else {
			st.setBigDecimal(index, decimal.getBigDecimal());
		}

	}

	@Override
	public int getSqlType() {
		return Types.NUMERIC;
	}

	@Override
	public Decimal nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
		BigDecimal value = rs.getBigDecimal(position);
		int scale = rs.getMetaData().getScale(position);
		if (value == null) {
			return null;
		}
		if (value.scale() < scale) {
			value = value.setScale(scale);
		}
		return new Decimal(value);
	}

	@Override
	public Class<Decimal> returnedClass() {
		return Decimal.class;
	}

}
