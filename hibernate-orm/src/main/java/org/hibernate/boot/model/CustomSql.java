/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model;

import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;

/**
 * Models the information for custom SQL execution defined as part of
 * the mapping for a primary/secondary table
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
