/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.jdbc.internal;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * @author Jan Schatteman
 */
public class EmptyStatementInspector implements StatementInspector {
	/**
	 * The singleton reference.
	 */
	public static final StatementInspector INSTANCE = new EmptyStatementInspector();

	@Override
	public String inspect(String sql) {
		return sql;
	}

	protected EmptyStatementInspector() {}
}
