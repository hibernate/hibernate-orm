/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import java.util.List;

/**
 * Common implicit name source traits for all constraint naming: FK, UK, index
 *
 * @author Steve Ebersole
 */
public sealed interface ImplicitConstraintNameSource
		extends ImplicitNameSource
		permits ImplicitIndexNameSource, ImplicitUniqueKeyNameSource, ImplicitForeignKeyNameSource {
	Identifier getTableName();
	List<Identifier> getColumnNames();
	Identifier getUserProvidedIdentifier();
	Kind kind();

	enum Kind {
		FOREIGN_KEY,
		UNIQUE_KEY,
		INDEX
	}
}
