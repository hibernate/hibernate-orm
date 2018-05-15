/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.query.NavigablePath;
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
public abstract class AbstractSqmCollectionElementReference extends AbstractSqmNavigableReference implements
		SqmCollectionElementReference {
	private final SqmPluralAttributeReference attributeReference;
	private final PluralPersistentAttribute navigable;
	private final NavigablePath navigablePath;

	public AbstractSqmCollectionElementReference(SqmPluralAttributeReference pluralAttributeBinding) {
		this.attributeReference = pluralAttributeBinding;
		this.navigable = pluralAttributeBinding.getReferencedNavigable();

		this.navigablePath = pluralAttributeBinding.getNavigablePath().append( "{elements}" );
	}

	public SqmPluralAttributeReference getPluralAttributeReference() {
		return attributeReference;
	}

	@Override
	public SqmPluralAttributeReference getSourceReference() {
		return getPluralAttributeReference();
	}

	@Override
	public Navigable getReferencedNavigable() {
		return getPluralAttributeReference().getReferencedNavigable().getPersistentCollectionDescriptor().getElementDescriptor();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public String asLoggableText() {
		return getNavigablePath().getFullPath();
	}

	@Override
	public ExpressableType getExpressableType() {
		return getPluralAttributeReference().getReferencedNavigable()
				.getPersistentCollectionDescriptor()
				.getElementDescriptor();
	}

	@Override
	public ExpressableType getInferableType() {
		return getExpressableType();
	}

	@Override
	public NavigableContainerReferenceInfo getNavigableContainerReferenceInfo() {
		return getPluralAttributeReference();
	}

	@Override
	public String getUniqueIdentifier() {
		// for most element classifications, the uid should point to the "collection table"...
		return getPluralAttributeReference().getUniqueIdentifier();
	}

	@Override
	public String getIdentificationVariable() {
		// for most element classifications, the "identification variable" (alias)
		// 		associated with elements is the identification variable for the collection reference
		return getPluralAttributeReference().getIdentificationVariable();
	}

	@Override
	public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
		// for most element classifications, there is none
		return null;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getReferencedNavigable().getJavaTypeDescriptor();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return getReferencedNavigable().getPersistenceType();
	}

	@Override
	public Class getJavaType() {
		return getReferencedNavigable().getJavaType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitPluralAttributeElementBinding( this );
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		return getPluralAttributeReference().getReferencedNavigable()
				.getPersistentCollectionDescriptor()
				.getElementDescriptor()
				.createSqmExpression( getPluralAttributeReference().getExportedFromElement(), getPluralAttributeReference(), context );
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
		return getPluralAttributeReference().getExportedFromElement();
	}
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
}
