/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
