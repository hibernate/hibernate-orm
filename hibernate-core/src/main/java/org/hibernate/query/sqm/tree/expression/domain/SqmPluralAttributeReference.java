/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.EntityTypeImplementor;
import org.hibernate.sql.ast.produce.metamodel.spi.NavigableContainerReferenceInfo;
import org.hibernate.query.sqm.NotYetImplementedException;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;

/**
 * Specialization of a "Navigable reference" for plural attributes.
 *
 * @author Steve Ebersole
 */
public class SqmPluralAttributeReference
		extends AbstractSqmAttributeReference<PluralPersistentAttribute>
		implements SqmNavigableContainerReference {

	public SqmPluralAttributeReference(SqmNavigableContainerReference lhs, PluralPersistentAttribute attribute) {
		super( lhs, attribute );
	}

	public SqmPluralAttributeReference(SqmAttributeJoin join) {
		super( join );
	}

	@Override
	public PluralPersistentAttribute getReferencedNavigable() {
		return super.getReferencedNavigable();
	}

	@Override
	public SqmAttributeJoin getExportedFromElement() {
		return (SqmAttributeJoin) super.getExportedFromElement();
	}


	@Override
	public NavigableContainerReferenceInfo getNavigableContainerReferenceInfo() {
		return getSourceReference();
	}

	@Override
	public String getTypeName() {
		return getReferencedNavigable().getTypeName();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return getReferencedNavigable().getPersistentCollectionMetadata().getPersistenceType();
	}

	@Override
	public Class getJavaType() {
		return getReferencedNavigable().getJavaType();
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
	public EntityTypeImplementor getIntrinsicSubclassEntityPersister() {
		throw new NotYetImplementedException(  );
	}
}
