/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.from;

import org.hibernate.HibernateException;

/**
 * Thrown when a {@link TableReference} cannot be resolved
 * for a table-name.
 *
 * @see ColumnReferenceQualifier
 *
 * @author Steve Ebersole
 */
public class UnknownTableReferenceException extends HibernateException {
	private final String tableExpression;

	public UnknownTableReferenceException(String tableExpression, String message) {
		super( message );
		this.tableExpression = tableExpression;
	}

	public String getTableExpression() {
		return tableExpression;
	}
}
