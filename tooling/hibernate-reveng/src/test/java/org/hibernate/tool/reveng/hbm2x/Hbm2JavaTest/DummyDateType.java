package org.hibernate.tool.reveng.hbm2x.Hbm2JavaTest;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Types;

public class DummyDateType implements UserType<Date> {

	public int[] sqlTypes() {
		return new int[]{Types.DATE};
	}

	@Nonnull
	public Class<Date> returnedClass() {
		return Date.class;
	}

	public boolean equals(@Nullable Date x, @Nullable Date y) throws HibernateException {
		return false;
	}

	public int hashCode(@Nonnull Date x) throws HibernateException {
		return 0;
	}

	@Nullable
	public Date deepCopy(@Nullable Date value) throws HibernateException {
		return null;
	}

	public boolean isMutable() {
		return false;
	}

	@Nullable
	public Serializable disassemble(@Nullable Date value) throws HibernateException {
		return null;
	}

	@Nullable
	public Date assemble(@Nullable Serializable cached, @Nullable Object owner)
			throws HibernateException {
		return null;
	}

	@Nullable
	public Date replace(
			@Nullable Date original,
			@Nullable Date target,
			@Nullable Object owner) throws HibernateException {
		return null;
	}

	@Override
	public int getSqlType() {
		return 0;
	}

}
