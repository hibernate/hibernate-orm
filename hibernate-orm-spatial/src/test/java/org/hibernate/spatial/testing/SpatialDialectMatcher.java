/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing;

import org.hibernate.dialect.Dialect;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.testing.Skip;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 1/13/12
 */
public class SpatialDialectMatcher implements Skip.Matcher {
	@Override
	public boolean isMatch() {
		return !( Dialect.getDialect() instanceof SpatialDialect );
	}
}
