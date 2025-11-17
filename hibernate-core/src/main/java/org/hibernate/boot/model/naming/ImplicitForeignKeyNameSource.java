/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public non-sealed interface ImplicitForeignKeyNameSource
		extends ImplicitConstraintNameSource {
	Identifier getReferencedTableName();
	List<Identifier> getReferencedColumnNames();

	@Override
	default Kind kind() {
		return Kind.FOREIGN_KEY;
	}
}
