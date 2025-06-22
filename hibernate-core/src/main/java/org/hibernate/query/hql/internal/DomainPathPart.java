/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.hql.internal;

import org.hibernate.query.hql.HqlLogging;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * Specialized "intermediate" SemanticPathPart for processing domain model paths/
 *
 * @author Steve Ebersole
 */
public class DomainPathPart implements SemanticPathPart {
	private SqmPath<?> currentPath;

	public DomainPathPart(SqmPath<?> basePath) {
		this.currentPath = basePath;
		assert currentPath != null;
	}

	SqmExpression<?> getSqmExpression() {
		return currentPath;
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		HqlLogging.QUERY_LOGGER.tracef(
				"Resolving DomainPathPart(%s) sub-part : %s",
				currentPath,
				name
		);
		currentPath = currentPath.resolvePathPart( name, isTerminal, creationState );
		if ( isTerminal ) {
			return currentPath;
		}
		else {
			return this;
		}
	}

	@Override
	public SqmPath<?> resolveIndexedAccess(
			SqmExpression<?> selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		return currentPath.resolveIndexedAccess( selector, isTerminal, creationState );
	}
}
