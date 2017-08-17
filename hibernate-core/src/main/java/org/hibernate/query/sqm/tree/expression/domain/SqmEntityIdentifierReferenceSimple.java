/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierSimple;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.NavigableContainerReferenceInfo;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmEntityIdentifierReferenceSimple
		extends AbstractSqmNavigableReference
		implements SqmEntityIdentifierReference {

	private final SqmEntityTypedReference source;
	private final EntityIdentifierSimple entityIdentifier;

	public SqmEntityIdentifierReferenceSimple(SqmEntityTypedReference source, EntityIdentifierSimple entityIdentifier) {
		this.source = source;
		this.entityIdentifier = entityIdentifier;
	}

	@Override
	public ExpressableType getExpressionType() {
		return entityIdentifier;
	}

	@Override
	public ExpressableType getInferableType() {
		return entityIdentifier;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitEntityIdentifierReference( this );
	}

	@Override
	public String asLoggableText() {
		return entityIdentifier.asLoggableText();
	}

	@Override
	public SqmNavigableContainerReference getSourceReference() {
		return source;
	}

	@Override
	public EntityIdentifierSimple getReferencedNavigable() {
		return entityIdentifier;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return source.getNavigablePath().append( entityIdentifier.getNavigableName() );
	}

	@Override
	public NavigableContainerReferenceInfo getNavigableContainerReferenceInfo() {
		return source;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return entityIdentifier.getJavaTypeDescriptor();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public Class getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryResult createQueryResult(
			Expression expression,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		return entityIdentifier.createQueryResult( expression, resultVariable, creationContext );
	}

	@Override
	public String getUniqueIdentifier() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public String getIdentificationVariable() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public EntityDescriptor getIntrinsicSubclassEntityMetadata() {
		throw new NotYetImplementedException(  );
	}
}
