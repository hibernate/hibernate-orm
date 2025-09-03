/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ops.spi;

import org.hibernate.Incubating;

import java.util.Set;

/**
 * An operation against the database, comprised of a
 * {@linkplain #getPrimaryOperation primary operation} and
 * zero-or-more {@linkplain SecondaryAction secondary actions}.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface DatabaseOperation<P extends PrimaryOperation> {
	/**
	 * The primary operation for the group.
	 */
	P getPrimaryOperation();

	/**
	 * The names of tables referenced or affected by this operation.
	 */
	Set<String> getAffectedTableNames();
}
