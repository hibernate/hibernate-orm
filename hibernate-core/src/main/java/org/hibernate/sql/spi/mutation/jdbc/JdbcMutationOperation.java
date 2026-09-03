/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.spi.mutation.jdbc;

import org.hibernate.SPI;
import org.hibernate.sql.exec.spi.JdbcOperation;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// [JdbcOperation] for a mapping-model mutation originating from a persistence
/// context flush.
///
/// A direct SQL AST translator obtains this operation from
/// [org.hibernate.sql.ast.spi.model.TableMutation#createMutationOperation(String, java.util.List)].
/// Do not implement this interface or use [org.hibernate.sql.exec.spi.JdbcOperations]
/// for a mapping-model mutation.
///
/// @since 8.0
/// @author Steve Ebersole
/// @see org.hibernate.sql.ast.spi.translation.SqlAstTranslator#translate
/// @see org.hibernate.sql.ast.spi.model.TableMutation#createMutationOperation(String, java.util.List)
@SPI({ USE, SUPPLY })
public interface JdbcMutationOperation extends JdbcOperation, PreparableMutationOperation {
}
