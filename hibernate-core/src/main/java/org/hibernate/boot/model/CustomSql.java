/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
public class CustomSql {
	private final String sql;
	private final boolean isCallable;
	private final ExecuteUpdateResultCheckStyle checkStyle;

	public CustomSql(String sql, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {
		this.sql = sql;
		this.isCallable = callable;
		this.checkStyle = checkStyle;
	}

	public String getSql() {
		return sql;
	}

	public boolean isCallable() {
		return isCallable;
	}

	public ExecuteUpdateResultCheckStyle getCheckStyle() {
		return checkStyle;
	}
}
