/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
