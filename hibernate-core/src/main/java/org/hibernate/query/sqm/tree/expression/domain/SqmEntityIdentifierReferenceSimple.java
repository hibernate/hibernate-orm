/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierSimple;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.NavigableContainerReferenceInfo;
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
	public ExpressableType getExpressableType() {
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
	public String getUniqueIdentifier() {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public String getIdentificationVariable() {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		throw new SemanticException( "Basic identifier cannot be rde-referenced" );
	}

	@Override
	public SqmRestrictedCollectionElementReference resolveIndexedAccess(
			SqmExpression selector,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		throw new UnsupportedOperationException(  );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// todo (6.0) : this is probably not right depending how we intend the logical join to element/index table
	@Override
	public SqmFrom getExportedFromElement() {
		return getSourceReference().getExportedFromElement();
	}
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
}
