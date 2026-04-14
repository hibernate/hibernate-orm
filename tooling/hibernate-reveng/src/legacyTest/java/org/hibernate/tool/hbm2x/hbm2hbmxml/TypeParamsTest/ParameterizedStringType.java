/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.hbm2x.hbm2hbmxml.TypeParamsTest;

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
