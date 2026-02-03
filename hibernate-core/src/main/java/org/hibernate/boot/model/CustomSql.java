/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model;

import org.hibernate.boot.jaxb.ResultCheckStyle;

/**
 * Models the information for custom SQL execution defined as part of
 * the mapping for a primary or secondary table.
 *
 * @author Steve Ebersole
 *
 * @deprecated Since hbm.xml support will go away
 */
@Deprecated(since = "7")
public record CustomSql(String sql, boolean callable, ResultCheckStyle checkStyle) {

	public String getSql() {
		return sql;
	}

	public boolean isCallable() {
		return callable;
	}

	public ResultCheckStyle getCheckStyle() {
		return checkStyle;
	}
}
