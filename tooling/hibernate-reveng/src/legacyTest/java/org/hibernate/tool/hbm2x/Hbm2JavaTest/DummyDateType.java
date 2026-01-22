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

package org.hibernate.tool.hbm2x.Hbm2JavaTest;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.*;

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
