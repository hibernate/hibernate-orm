/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// A query-language insert, update, or delete operation performed through JDBC.
///
/// This contract is distinct from [org.hibernate.sql.spi.mutation.MutationOperation], which represents a
/// mapping-model mutation originating from persistence-context work. Create
/// this operation with [JdbcOperations#queryMutation] when implementing a
/// custom SQL AST translator; do not implement it directly.
///
/// @since 8.0
/// @author Steve Ebersole
/// @see org.hibernate.sql.ast.spi.translation.SqlAstTranslator#translate
@SPI({ USE, SUPPLY })
public interface JdbcOperationQueryMutation extends JdbcOperationQuery, JdbcMutation {

}
