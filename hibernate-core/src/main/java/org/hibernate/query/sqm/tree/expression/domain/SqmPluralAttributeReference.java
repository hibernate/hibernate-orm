/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.persister.common.spi.PluralPersistentAttribute;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.persister.queryable.spi.NavigableSourceReferenceInfo;
import org.hibernate.query.sqm.NotYetImplementedException;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;

/**
 * Specialization of a "Navigable reference" for plural attributes.
 *
 * @author Steve Ebersole
 */
public class SqmPluralAttributeReference
		extends AbstractSqmAttributeReference<PluralPersistentAttribute>
		implements SqmNavigableSourceReference {

	public SqmPluralAttributeReference(SqmNavigableSourceReference lhs, PluralPersistentAttribute attribute) {
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
	public NavigableSourceReferenceInfo getSourceReferenceInfo() {
		return getSourceReference();
	}

	@Override
	public String getTypeName() {
		return getReferencedNavigable().getTypeName();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return getReferencedNavigable().getCollectionPersister().getPersistenceType();
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
	public EntityPersister getIntrinsicSubclassEntityPersister() {
		throw new NotYetImplementedException(  );
	}
}
