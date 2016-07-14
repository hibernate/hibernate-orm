/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.expression.instantiation;

import org.hibernate.sql.sqm.exec.results.spi.ReturnReader;

/**
 * @author Steve Ebersole
 */
class AliasedReturnReader {
	private final String alias;
	private final ReturnReader returnReader;

	public AliasedReturnReader(String alias, ReturnReader returnReader) {
		this.alias = alias;
		this.returnReader = returnReader;
	}

	public String getAlias() {
		return alias;
	}

	public ReturnReader getReturnReader() {
		return returnReader;
	}
}
