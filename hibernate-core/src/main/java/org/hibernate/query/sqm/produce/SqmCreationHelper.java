/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce;

import org.hibernate.query.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class SqmCreationHelper {
	public static NavigablePath buildRootNavigablePath(String base, String alias) {
		return alias == null
				? new NavigablePath( base )
				: new NavigablePath( base + '(' + alias + ')' );
	}

	public static NavigablePath buildSubNavigablePath(NavigablePath lhs, String base, String alias) {
		final String localPath = alias == null
				? base
				: base + '(' + alias + ')';
		return lhs.append( localPath );
	}

	private SqmCreationHelper() {
	}
}
