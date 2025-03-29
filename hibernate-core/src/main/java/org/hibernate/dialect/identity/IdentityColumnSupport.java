/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.identity;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.insert.GetGeneratedKeysDelegate;
import org.hibernate.persister.entity.EntityPersister;

/**
 * A set of operations providing support for identity columns
 * in a certain {@link Dialect SQL dialect}.
 *
 * @author Andrea Boriero
 *
 * @since 5.1
 */
public interface IdentityColumnSupport {
	/**
	 * Does this dialect support identity column key generation?
	 *
	 * @return True if IDENTITY columns are supported; false otherwise.
	 */
	boolean supportsIdentityColumns();

	/**
	 * Does the dialect support some form of inserting and selecting
	 * the generated IDENTITY value all in the same statement.
	 *
	 * @return True if the dialect supports selecting the just
	 * generated IDENTITY in the insert statement.
	 */
	boolean supportsInsertSelectIdentity();

	/**
	 * Whether this dialect have an Identity clause added to the data type or a
	 * completely separate identity data type
	 *
	 * @return boolean
	 */
	boolean hasDataTypeInIdentityColumn();

	/**
	 * Provided we {@link #supportsInsertSelectIdentity}, then attach the
	 * "select identity" clause to the  insert statement.
	 * <p>
	 * Note, if {@link #supportsInsertSelectIdentity} == false then
	 * the insert-string should be returned without modification.
	 *
	 * @param insertString The insert command
	 *
	 * @return The insert command with any necessary identity select
	 * clause attached.
	 *
	 * @deprecated Use {@link #appendIdentitySelectToInsert(String, String)} instead.
	 */
	@Deprecated( forRemoval = true, since = "6.5" )
	String appendIdentitySelectToInsert(String insertString);

	/**
	 * Provided we {@link #supportsInsertSelectIdentity}, then attach the
	 * "select identity" clause to the  insert statement.
	 * <p>
	 * Note, if {@link #supportsInsertSelectIdentity} == false then
	 * the insert-string should be returned without modification.
	 *
	 * @param identityColumnName The name of the identity column
	 * @param insertString The insert command
	 *
	 * @return The insert command with any necessary identity select
	 * clause attached.
	 */
	default String appendIdentitySelectToInsert(String identityColumnName, String insertString) {
		return appendIdentitySelectToInsert( insertString );
	}

	/**
	 * Get the select command to use to retrieve the last generated IDENTITY
	 * value for a particular table
	 *
	 * @param table The table into which the insert was done
	 * @param column The PK column.
	 * @param type The {@link java.sql.Types} type code.
	 *
	 * @return The appropriate select command
	 *
	 * @throws MappingException If IDENTITY generation is not supported.
	 */
	String getIdentitySelectString(String table, String column, int type) throws MappingException;


	/**
	 * The syntax used during DDL to define a column as being an IDENTITY of
	 * a particular type.
	 *
	 * @param type The {@link java.sql.Types} type code.
	 *
	 * @return The appropriate DDL fragment.
	 *
	 * @throws MappingException If IDENTITY generation is not supported.
	 */
	String getIdentityColumnString(int type) throws MappingException;


	/**
	 * The keyword used to insert a generated value into an identity column (or null).
	 * Need if the dialect does not support inserts that specify no column values.
	 *
	 * @return The appropriate keyword.
	 */
	String getIdentityInsertString();

	/**
	 * Is there a keyword used to insert a generated value into an identity column.
	 *
	 * @return {@code true} if the dialect does not support inserts that specify no column values.
	 */
	default boolean hasIdentityInsertKeyword() {
		return getIdentityInsertString() != null;
	}

	/**
	 * The delegate for dealing with {@code IDENTITY} columns using
	 * {@link java.sql.PreparedStatement#getGeneratedKeys}.
	 *
	 * @param persister The persister
	 *
	 * @return the dialect-specific {@link GetGeneratedKeysDelegate}
	 */
	GetGeneratedKeysDelegate buildGetGeneratedKeysDelegate(EntityPersister persister);
}
