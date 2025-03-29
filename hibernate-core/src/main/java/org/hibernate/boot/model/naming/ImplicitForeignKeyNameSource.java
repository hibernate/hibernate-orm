/*
 * SPDX-License-Identifier: Apache-2.0
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
