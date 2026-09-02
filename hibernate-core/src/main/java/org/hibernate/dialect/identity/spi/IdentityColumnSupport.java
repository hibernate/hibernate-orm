/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.identity.spi;

import org.hibernate.MappingException;
import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Defines the identity-column DDL, insert syntax, and identity-specific value
/// retrieval semantics of a [org.hibernate.dialect.Dialect].
///
/// Providers may implement this contract directly or extend
/// [IdentityColumnSupportBase], and must supply one stable non-null instance
/// from [org.hibernate.dialect.Dialect#getIdentityColumnSupport()]. Implementations describe SQL and
/// the semantic retrieval choice only; they must not construct Hibernate
/// mutation delegates or depend on entity persisters.
///
/// @see org.hibernate.dialect.Dialect#getIdentityColumnSupport()
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface IdentityColumnSupport {
	/// Whether identity-column key generation is supported.
	boolean supportsIdentityColumns();

	/// Whether identity-selection SQL may be combined with or appended to the
	/// insert SQL.
	boolean supportsInsertSelectIdentity();

	/// Whether the identity declaration follows an existing SQL data type.
	boolean hasDataTypeInIdentityColumn();

	/// Transform an insert so its execution also returns the generated identity.
	/// Return the input unchanged when no transformation is required.
	String appendIdentitySelectToInsert(String identityColumnName, String insertString);

	/// Render the separate statement used to select the last generated identity.
	///
	/// @throws MappingException when separate identity selection is unsupported
	String getIdentitySelectString(String table, String column, int jdbcTypeCode) throws MappingException;

	/// Render the DDL fragment which declares an identity column.
	///
	/// @throws MappingException when identity columns are unsupported or the JDBC
	/// type is not a valid identity type
	String getIdentityColumnString(int jdbcTypeCode) throws MappingException;

	/// The value keyword written for an identity column, or `null` when the
	/// identity column is omitted from the insert.
	String getIdentityInsertString();

	/// Whether [#getIdentityInsertString()] supplies a value keyword.
	default boolean hasIdentityInsertKeyword() {
		return getIdentityInsertString() != null;
	}

	/// Select the identity-specific retrieval behavior Hibernate should execute.
	IdentityValueRetrieval getIdentityValueRetrieval();
}
