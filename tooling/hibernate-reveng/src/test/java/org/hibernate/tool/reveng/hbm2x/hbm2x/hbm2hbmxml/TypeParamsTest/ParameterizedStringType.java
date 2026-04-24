/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.hbm2hbmxml.TypeParamsTest;

import java.sql.Types;
import java.util.Properties;

import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

/**
 * A simple parameterized UserType that stores strings.
 * Used to test round-tripping of {@code <type>} and {@code <param>} elements.
 */
public class ParameterizedStringType implements UserType<String>, ParameterizedType {

	@Override
	public void setParameterValues(Properties parameters) {
	}

	@Override
	public int getSqlType() {
		return Types.VARCHAR;
	}

	@Override
	public Class<String> returnedClass() {
		return String.class;
	}

	@Override
	public String deepCopy(String value) {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

}
