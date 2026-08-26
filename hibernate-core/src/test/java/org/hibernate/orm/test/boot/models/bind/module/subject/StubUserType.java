/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.module.subject;

import java.sql.Types;

import org.hibernate.usertype.UserType;

public class StubUserType implements UserType<StubDomainType> {
	@Override
	public int getSqlType() {
		return Types.VARCHAR;
	}

	@Override
	public Class<StubDomainType> returnedClass() {
		return StubDomainType.class;
	}

	@Override
	public StubDomainType deepCopy(StubDomainType value) {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}
}
