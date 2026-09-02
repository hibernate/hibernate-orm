/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.identity.spi;

import org.hibernate.MappingException;
import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Provider base for implementing identity-column support.
///
/// The inherited behavior represents a database without identity columns.
/// Override only the operations supported by the database, and supply the
/// resulting stable instance from
/// [org.hibernate.dialect.Dialect#getIdentityColumnSupport()].
///
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT })
public class IdentityColumnSupportBase implements IdentityColumnSupport {
	/// Reusable support value for a database without identity columns.
	public static final IdentityColumnSupport NONE = new IdentityColumnSupportBase();

	/// Construct identity support initialized with the conservative defaults.
	///
	/// @since 8.0
	public IdentityColumnSupportBase() {
	}

	@Override
	public boolean supportsIdentityColumns() {
		return false;
	}

	@Override
	public boolean supportsInsertSelectIdentity() {
		return false;
	}

	@Override
	public boolean hasDataTypeInIdentityColumn() {
		return true;
	}

	@Override
	public String appendIdentitySelectToInsert(String identityColumnName, String insertString) {
		return insertString;
	}

	@Override
	public String getIdentitySelectString(String table, String column, int jdbcTypeCode) throws MappingException {
		throw unsupportedIdentityGeneration();
	}

	@Override
	public String getIdentityColumnString(int jdbcTypeCode) throws MappingException {
		throw unsupportedIdentityGeneration();
	}

	@Override
	public String getIdentityInsertString() {
		return null;
	}

	@Override
	public IdentityValueRetrieval getIdentityValueRetrieval() {
		return IdentityValueRetrieval.INFERRED_GENERATED_KEYS;
	}

	private MappingException unsupportedIdentityGeneration() {
		return new MappingException( getClass().getName() + " does not support identity key generation" );
	}
}
