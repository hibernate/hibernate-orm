/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface ImplicitForeignKeyNameSource extends ImplicitConstraintNameSource {
	Identifier getReferencedTableName();
	List<Identifier> getReferencedColumnNames();
}
