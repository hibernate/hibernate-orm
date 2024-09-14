/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql;

import org.hibernate.Internal;

/**
 * For a complete predicate.  E.g. {@link org.hibernate.annotations.SQLRestriction}
 *
 * @author Steve Ebersole
 */
@Internal
public class CompleteRestriction implements Restriction {
	private final String predicate;

	public CompleteRestriction(String predicate) {
		this.predicate = predicate;
	}

	@Override
	public void render(StringBuilder sqlBuffer, RestrictionRenderingContext context) {
		sqlBuffer.append( predicate );
	}
}
