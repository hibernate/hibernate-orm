/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast;

import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

/**
 * @author Steve Ebersole
 */
public interface CustomSqlMutation<O extends JdbcMutationOperation> extends TableMutation<O> {
	/**
	 * The custom SQL provided by the mapping
	 */
	String getCustomSql();

	/**
	 * Whether {@link #getCustomSql()} represents a callable (function/procedure)
	 */
	boolean isCallable();
}
