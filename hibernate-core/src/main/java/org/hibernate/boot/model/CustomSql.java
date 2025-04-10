/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model;

import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;

/**
 * Models the information for custom SQL execution defined as part of
 * the mapping for a primary or secondary table.
 *
 * @author Steve Ebersole
 */
public record CustomSql(String sql, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {

	@Deprecated(since = "7")
	public String getSql() {
		return sql;
	}

	@Deprecated(since = "7")
	public boolean isCallable() {
		return callable;
	}

	@Deprecated(since = "7")
	public ExecuteUpdateResultCheckStyle getCheckStyle() {
		return checkStyle;
	}
}
