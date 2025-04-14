/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.internal;

import org.hibernate.sql.ast.SqlParameterInfo;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @since 7.0
 */
public class SqlParameterInfoImpl implements SqlParameterInfo {

	private final Map<JdbcParameter, Integer> parameterIdMap;

	public SqlParameterInfoImpl() {
		this(new IdentityHashMap<>());
	}

	public SqlParameterInfoImpl(Map<JdbcParameter, Integer> parameterIdMap) {
		this.parameterIdMap = parameterIdMap;
	}

	@Override
	public int getParameterId(JdbcParameter jdbcParameter) {
		final Integer id = parameterIdMap.get( jdbcParameter );
		if ( id == null ) {
			final int newId = parameterIdMap.size();
			parameterIdMap.put( jdbcParameter, newId );
			return newId;
		}
		return id;
	}
}
