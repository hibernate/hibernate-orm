/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import java.util.List;

/**
 * Common implicit name source traits for all constraint naming: FK, UK, index
 *
 * @author Steve Ebersole
 */
public interface ImplicitConstraintNameSource extends ImplicitNameSource {
	Identifier getTableName();
	List<Identifier> getColumnNames();
	Identifier getUserProvidedIdentifier();
}
