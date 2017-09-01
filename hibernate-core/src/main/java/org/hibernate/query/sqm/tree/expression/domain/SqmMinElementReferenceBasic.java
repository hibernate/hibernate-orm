/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.BasicCollectionElement;
import org.hibernate.query.sqm.NotYetImplementedException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;

/**
 * @author Steve Ebersole
 */
public class SqmMinElementReferenceBasic extends AbstractSpecificSqmElementReference implements SqmMinElementReference {
	public SqmMinElementReferenceBasic(SqmPluralAttributeReference pluralAttributeBinding) {
		super( pluralAttributeBinding );
	}

	@Override
	public BasicCollectionElement getExpressionType() {
		return (BasicCollectionElement) getPluralAttributeBinding().getReferencedNavigable().getPersistentCollectionMetadata().getElementDescriptor();
	}

	@Override
	public BasicCollectionElement getInferableType() {
		return getExpressionType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public String asLoggableText() {
		return "MINELEMENT(" + getPluralAttributeBinding().asLoggableText() + ")";
	}

	@Override
	public BasicCollectionElement getReferencedNavigable() {
		return (BasicCollectionElement) super.getReferencedNavigable();
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryResult createQueryResult(
			Expression expression,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		return getReferencedNavigable().createQueryResult( expression, resultVariable, creationContext );
	}
}
