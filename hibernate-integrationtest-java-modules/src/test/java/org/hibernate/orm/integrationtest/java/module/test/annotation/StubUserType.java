package org.hibernate.orm.integrationtest.java.module.test.annotation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.sql.Types;

import org.hibernate.usertype.UserType;

public class StubUserType implements UserType<StubBasicType> {
	@Override
	public int getSqlType() {
		return Types.VARCHAR;
	}

	@Override
	@Nonnull
	public Class<StubBasicType> returnedClass() {
		return StubBasicType.class;
	}

	@Override
	@Nullable
	public StubBasicType deepCopy(@Nullable StubBasicType value) {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}
}
