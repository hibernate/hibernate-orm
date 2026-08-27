/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.integrationtest.java.module.test.annotation;

import java.sql.Types;

import org.hibernate.usertype.UserType;

public class StubUserType implements UserType<StubBasicType> {
	@Override
	public int getSqlType() {
		return Types.VARCHAR;
	}

	@Override
	public Class<StubBasicType> returnedClass() {
		return StubBasicType.class;
	}

	@Override
	public StubBasicType deepCopy(StubBasicType value) {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}
}
