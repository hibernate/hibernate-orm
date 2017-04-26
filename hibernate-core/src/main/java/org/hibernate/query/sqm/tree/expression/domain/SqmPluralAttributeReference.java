/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.persister.common.spi.PluralPersistentAttribute;
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

}
