/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import java.util.List;
import java.util.Set;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// A command to perform through JDBC, normally with a
/// [java.sql.PreparedStatement] or [java.sql.CallableStatement].
///
/// The command text is ordinarily SQL, but may use another language understood
/// by the configured JDBC driver. Likewise, affected table names may identify
/// backend-equivalent query spaces such as document collections.
///
/// Custom SQL AST translators supply operations to Hibernate through
/// [org.hibernate.sql.ast.spi.translation.SqlAstTranslator#translate]. Build
/// query operations with [JdbcOperations]; do not implement operation contracts
/// or instantiate Hibernate's internal implementations.
///
/// @since 8.0
/// @author Steve Ebersole
/// @see org.hibernate.sql.ast.spi.translation.SqlAstTranslator#translate
/// @see org.hibernate.sql.ast.spi.model.TableMutation#createMutationOperation(String, List)
@SPI({ USE, SUPPLY })
public interface JdbcOperation {
	/// The command text to execute through JDBC. This is ordinarily SQL, but may
	/// use another language understood by the configured JDBC driver.
	String getSqlString();

	/// The relational tables or backend-equivalent query spaces referred to by
	/// this operation.
	Set<String> getAffectedTableNames();

	/**
	 * The list of parameter binders for the generated PreparedStatement.
	 */
	List<? extends JdbcParameterBinder> getParameterBinders();
}
