/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.LockOptions;
import org.hibernate.dialect.DatabaseVersion;

/**
 * An SQL dialect for MariaDB 10.3 and later, provides sequence support, lock-timeouts, etc.
 *
 * @author Philippe Marschall
 *
 * @deprecated use {@code MariaDBLegacyDialect(1030)}
 */
@Deprecated
public class MariaDB103Dialect extends MariaDBLegacyDialect {

	public MariaDB103Dialect() {
		super( DatabaseVersion.make( 10, 3 ) );
	}

	@Override
	public String getWriteLockString(int timeout) {
		if ( timeout == LockOptions.NO_WAIT ) {
			return getForUpdateNowaitString();
		}

		if ( timeout > 0 ) {
			return getForUpdateString() + " wait " + getTimeoutInSeconds( timeout );
		}

		return getForUpdateString();
	}

}
