/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.path.spi;

import org.hibernate.query.sqm.produce.internal.hql.SemanticQueryBuilder;
import org.hibernate.query.sqm.produce.spi.ParsingContext;
import org.hibernate.query.sqm.produce.spi.QuerySpecProcessingState;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractStandardNavigableJoinBuilder extends AbstractNavigableJoinBuilder {
	private final SemanticQueryBuilder queryBuilder;

	public AbstractStandardNavigableJoinBuilder(SemanticQueryBuilder queryBuilder) {
		this.queryBuilder = queryBuilder;
	}

	@Override
	protected ParsingContext getParsingContext() {
		return queryBuilder.getParsingContext();
	}

	@Override
	protected QuerySpecProcessingState getQuerySpecProcessingState() {
		return queryBuilder.getQuerySpecProcessingState();
	}
}
