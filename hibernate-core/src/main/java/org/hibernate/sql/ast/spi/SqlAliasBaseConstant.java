/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

/**
 * A SqlAliasBase that always returns the same constant.
 *
 * @author Christian Beikov
 */
public class SqlAliasBaseConstant implements SqlAliasBase {
	private final String constant;

	public SqlAliasBaseConstant(String constant) {
		this.constant = constant;
	}

	@Override
	public String getAliasStem() {
		return constant;
	}

	@Override
	public String generateNewAlias() {
		return constant;
	}

	@Override
	public String toString() {
		return "SqlAliasBase(" + constant + ")";
	}
}
