/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
