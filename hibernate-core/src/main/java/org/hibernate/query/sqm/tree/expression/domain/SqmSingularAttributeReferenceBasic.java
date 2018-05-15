/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import java.util.Locale;

import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeBasic;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class SqmSingularAttributeReferenceBasic extends AbstractSqmSingularAttributeReference {
	public SqmSingularAttributeReferenceBasic(
			SqmNavigableContainerReference containerReference,
			SingularPersistentAttributeBasic boundNavigable,
			SqmCreationContext creationContext) {
		super( containerReference, boundNavigable );
	}

	@Override
	public SingularPersistentAttributeBasic getReferencedNavigable() {
		return (SingularPersistentAttributeBasic) super.getReferencedNavigable();
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return getSourceReference().getExportedFromElement();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public String getUniqueIdentifier() {
		return null;
	}

	@Override
	public String getIdentificationVariable() {
		return null;
	}

	@Override
	public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
		return null;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitBasicValuedSingularAttribute( this );
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"SqmSingularAttributeReferenceBasic(%s) %s -> %s",
				getNavigablePath(),
				getNavigableContainerReferenceInfo().getUniqueIdentifier(),
				getReferencedNavigable().getName()
		);
	}
}
