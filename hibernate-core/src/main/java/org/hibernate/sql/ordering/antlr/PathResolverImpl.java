/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ordering.antlr;

import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.produce.internal.hql.navigable.PathResolverBasicImpl;
import org.hibernate.query.sqm.produce.spi.ParsingContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class PathResolverImpl extends PathResolverBasicImpl {
	public PathResolverImpl(ParsingContext parsingContext, SqmFrom fromElement) {
		super( new PathResolutionContextImpl( parsingContext, fromElement ) );
	}

	@Override
	protected void validateIntermediateAttributeJoin(
			SqmNavigableContainerReference sourceBinding,
			Navigable joinedAttributeDescriptor) {
		if ( joinedAttributeDescriptor instanceof EntityValuedNavigable ) {
			throw new SemanticException(
					"@javax.persistence.OrderBy not allowed to implicitly join " +
							"entity-valued attributes"
			);
		}
	}
}
