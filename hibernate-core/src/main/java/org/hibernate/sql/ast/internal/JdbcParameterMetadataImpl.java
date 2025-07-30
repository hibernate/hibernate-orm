/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.internal;

import org.hibernate.sql.ast.JdbcParameterMetadata;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;

import java.util.Map;

/**
 * @since 7.0
 */
public class JdbcParameterMetadataImpl implements JdbcParameterMetadata {

	private final Map<JdbcParameter, Integer> parameterIdMap;
	private final int parameterIdCount;

	public JdbcParameterMetadataImpl(Map<JdbcParameter, Integer> parameterIdMap, int parameterIdCount) {
		this.parameterIdMap = parameterIdMap;
		this.parameterIdCount = parameterIdCount;
	}

	@Override
	public int getParameterId(JdbcParameter jdbcParameter) {
		final Integer id = parameterIdMap.get( jdbcParameter );
		if ( id == null ) {
			throw new IllegalArgumentException( "JdbcParameter " + jdbcParameter + " not found in SqlParameterInfoImpl." );
		}
		return id;
	}

	@Override
	public int getParameterIdCount() {
		return parameterIdCount;
	}
}
