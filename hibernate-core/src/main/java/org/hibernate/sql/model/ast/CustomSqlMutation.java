/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
