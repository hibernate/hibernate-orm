/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.internal;

import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * The standard ParameterMarkerStrategy based on the standard JDBC {@code ?} marker
 *
 * @author Steve Ebersole
 */
public class ParameterMarkerStrategyStandard implements ParameterMarkerStrategy {
	/**
	 * Singleton access
	 */
	public static final ParameterMarkerStrategyStandard INSTANCE = new ParameterMarkerStrategyStandard();

	@Override
	public String createMarker(int position, JdbcType jdbcType) {
		return "?";
	}

	public static boolean isStandardRenderer(ParameterMarkerStrategy check) {
		return check == null || ParameterMarkerStrategyStandard.class.equals( check.getClass() );
	}
}
