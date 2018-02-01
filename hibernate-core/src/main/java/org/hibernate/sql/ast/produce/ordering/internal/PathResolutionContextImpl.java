/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.ordering.internal;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.produce.internal.FromElementBuilder;
import org.hibernate.query.sqm.produce.spi.FromElementLocator;
import org.hibernate.query.sqm.produce.spi.ParsingContext;
import org.hibernate.query.sqm.produce.spi.ResolutionContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class PathResolutionContextImpl implements ResolutionContext, FromElementLocator {
	private final ParsingContext parsingContext;
	private final SqmFrom sqmFrom;

	public PathResolutionContextImpl(
			ParsingContext parsingContext,
			SqmFrom sqmFrom) {
		this.parsingContext = parsingContext;
		this.sqmFrom = sqmFrom;
	}

	@Override
	public FromElementLocator getFromElementLocator() {
		throw new SemanticException( "@javax.persistence.OrderBy not allowed to implicitly join entity-valued attributes" );
	}

	@Override
	public FromElementBuilder getFromElementBuilder() {
		throw new SemanticException( "@javax.persistence.OrderBy not allowed to generate from-elements" );
	}

	@Override
	public ParsingContext getParsingContext() {
		return parsingContext;
	}

	@Override
	public SqmNavigableReference findNavigableReferenceByIdentificationVariable(String identificationVariable) {
		// order-by fragments should generally not reference identification variables
		throw new SemanticException( "@javax.persistence.OrderBy fragments should no reference identification variable" );
	}

	@Override
	public SqmNavigableReference findNavigableReferenceExposingAttribute(String attributeName) {
		assert StringHelper.isNotEmpty( attributeName );

		// validate that the attribute name is defined by the from-element
		final NavigableContainer navigableContainer = (NavigableContainer) sqmFrom.getNavigableReference().getReferencedNavigable();
		final Navigable navigable = navigableContainer.findNavigable( attributeName );
		if ( navigable == null ) {
			throw new SemanticException(
					"Could not resolve attribute named `" + attributeName +
							"` relative to `" + navigableContainer.getNavigableRole() +
							"` as part of @javax.persistence.OrderBy fragment"
			);
		}

		return sqmFrom.getNavigableReference();
	}
}
