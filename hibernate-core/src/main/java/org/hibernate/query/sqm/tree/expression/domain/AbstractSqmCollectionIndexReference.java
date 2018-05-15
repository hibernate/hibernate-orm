/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.collection.spi.CollectionClassification;
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
public abstract class AbstractSqmCollectionIndexReference
		extends AbstractSqmNavigableReference
		implements SqmCollectionIndexReference {

	private final SqmPluralAttributeReference attributeReference;
	private final PluralPersistentAttribute pluralAttributeReference;
	private final NavigablePath propertyPath;

	public AbstractSqmCollectionIndexReference(SqmPluralAttributeReference pluralAttributeBinding) {
		this.attributeReference = pluralAttributeBinding;
		this.pluralAttributeReference = pluralAttributeBinding.getReferencedNavigable();

		assert pluralAttributeReference.getPersistentCollectionDescriptor().getCollectionClassification() == CollectionClassification.MAP
				|| pluralAttributeReference.getPersistentCollectionDescriptor().getCollectionClassification() == CollectionClassification.LIST;

		this.propertyPath = pluralAttributeBinding.getNavigablePath().append( "{keys}" );
	}

	public SqmPluralAttributeReference getPluralAttributeReference() {
		return attributeReference;
	}

	@Override
	public ExpressableType getExpressableType() {
		return getPluralAttributeReference().getReferencedNavigable().getPersistentCollectionDescriptor().getIndexDescriptor();
	}

	@Override
	public ExpressableType getInferableType() {
		return getExpressableType();
	}

	@Override
	public SqmPluralAttributeReference getSourceReference() {
		return attributeReference;
	}

	@Override
	public Navigable getReferencedNavigable() {
		return pluralAttributeReference.getPersistentCollectionDescriptor().getIndexDescriptor();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return propertyPath;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitMapKeyBinding( this );
	}

	@Override
	public String asLoggableText() {
		return "KEY(" + attributeReference.asLoggableText() + ")";
	}


	@Override
	public NavigableContainerReferenceInfo getNavigableContainerReferenceInfo() {
		return getPluralAttributeReference();
	}

	@Override
	public String getUniqueIdentifier() {
		// for most index classifications, the uid should point to the "collection table"...
		return getPluralAttributeReference().getUniqueIdentifier();
	}

	@Override
	public String getIdentificationVariable() {
		// for most index classifications, the "identification variable" (alias)
		// 		associated with the index should point to the identification variable
		// 		for the collection reference
		return getPluralAttributeReference().getIdentificationVariable();
	}

	@Override
	public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
		// for most index classifications, there is none
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
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		return getPluralAttributeReference().getReferencedNavigable()
				.getPersistentCollectionDescriptor()
				.getIndexDescriptor()
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
