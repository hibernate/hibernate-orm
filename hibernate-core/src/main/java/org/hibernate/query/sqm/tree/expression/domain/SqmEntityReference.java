/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.NavigableContainerReferenceInfo;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class SqmEntityReference extends AbstractSqmNavigableReference
		implements SqmNavigableReference, SqmNavigableContainerReference, SqmEntityTypedReference {
	private static final Logger log = Logger.getLogger( SqmEntityReference.class );

	private final SqmNavigableContainerReference containerReference;
	private final EntityValuedExpressableType entityReference;
	private NavigablePath propertyPath;

	private SqmFrom exportedFromElement;

	public SqmEntityReference(EntityValuedExpressableType entityReference) {
		this.entityReference = entityReference;
		this.containerReference = null;
		this.propertyPath = new NavigablePath( null, entityReference.getEntityName() );
	}

	public SqmEntityReference(SqmNavigableContainerReference containerReference, EntityValuedExpressableType entityReference) {
		this.containerReference = containerReference;
		this.entityReference = entityReference;

		this.propertyPath = new NavigablePath( containerReference.getNavigablePath(), entityReference.getEntityName() );
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return exportedFromElement;
	}

	@Override
	public void injectExportedFromElement(SqmFrom sqmFrom) {
		log.debugf(
				"Injecting SqmFrom [%s] into EntityBindingImpl [%s], was [%s]",
				sqmFrom,
				this,
				this.exportedFromElement
		);
		exportedFromElement = sqmFrom;
		propertyPath = new NavigablePath( null, entityReference.getEntityName() + "(" + sqmFrom.getIdentificationVariable() + ")" );
	}

	@Override
	public SqmNavigableContainerReference getSourceReference() {
		return containerReference;
	}

	@Override
	public EntityValuedExpressableType getReferencedNavigable() {
		return entityReference;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return propertyPath;
	}

	@Override
	public EntityValuedExpressableType getExpressableType() {
		return entityReference;
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
		throw new NotYetImplementedException(  );
	}

	@Override
	public String asLoggableText() {
		return entityReference.asLoggableText();
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
	public EntityDescriptor getIntrinsicSubclassEntityMetadata() {
		return exportedFromElement.getIntrinsicSubclassEntityMetadata();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.ENTITY;
	}

	@Override
	public NavigableContainerReferenceInfo getNavigableContainerReferenceInfo() {
		return containerReference;
	}

}
