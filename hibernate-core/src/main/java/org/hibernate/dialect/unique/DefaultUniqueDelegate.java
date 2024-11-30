/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.unique;

import org.hibernate.dialect.Dialect;

/**
 * @deprecated use {@link org.hibernate.dialect.unique.AlterTableUniqueDelegate}
 */
@Deprecated(since="6.2", forRemoval = true)
public class DefaultUniqueDelegate extends AlterTableUniqueDelegate {
	public DefaultUniqueDelegate(Dialect dialect) {
		super(dialect);
	}
}
