/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.Hbm2JavaTest;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Types;

public class DummyDateType implements UserType<Date> {

	public int[] sqlTypes() {
		return new int[]{Types.DATE};
	}

	public Class<Date> returnedClass() {
		return Date.class;
	}

	public boolean equals(Date x, Date y) throws HibernateException {
		return false;
	}

	public int hashCode(Date x) throws HibernateException {
		return 0;
	}

	public Date deepCopy(Date value) throws HibernateException {
		return null;
	}

	public boolean isMutable() {
		return false;
	}

	public Serializable disassemble(Date value) throws HibernateException {
		return null;
	}

	public Date assemble(Serializable cached, Object owner)
			throws HibernateException {
		return null;
	}

	public Date replace(
			Date original,
			Date target,
			Object owner) throws HibernateException {
		return null;
	}

	@Override
	public int getSqlType() {
		return 0;
	}

}
