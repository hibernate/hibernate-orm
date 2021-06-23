/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;

/**
 * The DialectResolver implementation for community maintained dialects
 *
 * @author Christian Beikov
 */
public class CommunityDialectResolver implements DialectResolver {

	@Override
	public Dialect resolveDialect(DialectResolutionInfo info) {
		for ( CommunityDatabase database : CommunityDatabase.values() ) {
			if ( database.matchesResolutionInfo( info ) ) {
				return database.createDialect( info );
			}
		}

		return null;
	}

}
