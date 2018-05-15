/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.NavigableContainerReferenceInfo;

import org.jboss.logging.Logger;

/**
 * Defines a reference to an entity that is the root of a TableSpace:
 *
 * 		* Root
 * 		* Cross-join
 * 		* Entity-join
 *
 * @author Steve Ebersole
 */
public class SqmEntityReference extends AbstractSqmNavigableReference
		implements SqmNavigableReference, SqmNavigableContainerReference, SqmEntityTypedReference {
	private static final Logger log = Logger.getLogger( SqmEntityReference.class );

	private final EntityTypeDescriptor entityDescriptor;
	private final SqmFrom exportedFromElement;

	private final NavigablePath propertyPath;

	public SqmEntityReference(
			EntityTypeDescriptor entityDescriptor,
			SqmFrom sqmFrom,
			SqmCreationContext creationContext) {
		this.entityDescriptor = entityDescriptor;
		this.exportedFromElement = sqmFrom;
		this.propertyPath = new NavigablePath( null, this.entityDescriptor.getEntityName() + '(' + sqmFrom.getIdentificationVariable() + ')' );
	}

	public EntityTypeDescriptor getEntityDescriptor() {
		return entityDescriptor;
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return exportedFromElement;
	}

	@Override
	public SqmNavigableContainerReference getSourceReference() {
		return null;
	}

	@Override
	public EntityValuedExpressableType getReferencedNavigable() {
		return entityDescriptor;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return propertyPath;
	}

	@Override
	public EntityValuedExpressableType getExpressableType() {
		return getReferencedNavigable();
	}

	@Override
	public EntityValuedExpressableType getInferableType() {
		return getExpressableType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T accept(SemanticQueryWalker<T> walker) {
		// todo (6.0) : this needs to vary based on who/what exactly created this entity-reference
		//		- one option, in keeping with visitation, would be to have specific subclasses of
		//			SqmEntityReference with different SemanticQueryWalker target methods
		//
		//	e.g., given `select p from Person p` we'd need to call `walker.visitRootEntityFromElement`
		//		whereas with `select p.mate from Person p` we'd need to call `.visitEntityValuedSingularAttribute`
		//
		//	in the latter case we'd need to render the FK columns as well

		// for now, here are the "branches of visitation"...
		return walker.visitRootEntityReference( this );
	}

	@Override
	public String asLoggableText() {
		return entityDescriptor.asLoggableText();
	}

	@Override
	public String getUniqueIdentifier() {
		return exportedFromElement.getUniqueIdentifier();
	}

	@Override
	public String getIdentificationVariable() {
		return exportedFromElement.getIdentificationVariable();
	}

	@Override
	public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
		return exportedFromElement.getIntrinsicSubclassEntityMetadata();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.ENTITY;
	}

	@Override
	public NavigableContainerReferenceInfo getNavigableContainerReferenceInfo() {
		return null;
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		return entityDescriptor.getEntityDescriptor()
				.findNavigable( name )
				.createSqmExpression( exportedFromElement, this, context );
	}

	@Override
	public SqmRestrictedCollectionElementReference resolveIndexedAccess(
			SqmExpression selector,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		throw new SemanticException( "Entity reference cannot be index-accessed" );
	}
}
