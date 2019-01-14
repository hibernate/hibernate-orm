/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.PostgreSQL10IdentityColumnSupport;

/**
 * An SQL dialect for Postgres 10 and later.
 */
public class PostgreSQL10Dialect extends PostgreSQL95Dialect {

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new PostgreSQL10IdentityColumnSupport();
	}
}
