/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.spi.NavigablePath;

public class PathHelper {
	public static NavigablePath append(SqmPath<?> lhs, SqmPathSource<?> rhs, SqmPathSource<?> intermediatePathSource) {
		return intermediatePathSource == null
				? lhs.getNavigablePath().append( rhs.getPathName() )
				: lhs.getNavigablePath().append( intermediatePathSource.getPathName() ).append( rhs.getPathName() );
	}
}
