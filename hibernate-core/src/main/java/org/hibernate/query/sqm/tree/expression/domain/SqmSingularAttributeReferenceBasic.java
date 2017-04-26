/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.persister.common.internal.SingularPersistentAttributeBasic;
import org.hibernate.query.sqm.domain.type.SqmDomainTypeBasic;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;

/**
 * @author Steve Ebersole
 */
public class SqmSingularAttributeReferenceBasic extends AbstractSqmSingularAttributeReference {
	public SqmSingularAttributeReferenceBasic(
			SqmNavigableSourceReference sourceBinding,
			SingularPersistentAttributeBasic boundNavigable) {
		super( sourceBinding, boundNavigable );
	}

	public SqmSingularAttributeReferenceBasic(SqmAttributeJoin fromElement) {
		super( fromElement );
	}

	@Override
	public SingularPersistentAttributeBasic getReferencedNavigable() {
		return (SingularPersistentAttributeBasic) super.getReferencedNavigable();
	}

	@Override
	public SqmDomainTypeBasic getExportedDomainType() {
		return (SqmDomainTypeBasic) getReferencedNavigable().getExportedDomainType();
	}
}
