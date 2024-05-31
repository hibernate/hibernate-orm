/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
