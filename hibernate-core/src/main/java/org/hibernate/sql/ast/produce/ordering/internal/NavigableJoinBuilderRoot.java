/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.ordering.internal;

import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.produce.internal.hql.SemanticQueryBuilder;
import org.hibernate.query.sqm.produce.path.spi.AbstractStandardNavigableJoinBuilder;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;

/**
 * @author Steve Ebersole
 */
public class NavigableJoinBuilderRoot extends AbstractStandardNavigableJoinBuilder {
	public NavigableJoinBuilderRoot(SemanticQueryBuilder queryBuilder) {
		super( queryBuilder );
	}

	@Override
	public void buildNavigableJoinIfNecessary(
			SqmNavigableReference navigableReference,
			boolean isTerminal) {
		// todo (6.0) : I'm not so sure JPA says this is illegal
		if ( !isTerminal ) {
			throw new SemanticException(
					"@javax.persistence.OrderBy not allowed to implicitly join " +
							"entity-valued attributes"
			);
		}

		super.buildNavigableJoinIfNecessary( navigableReference, isTerminal );
	}
}
