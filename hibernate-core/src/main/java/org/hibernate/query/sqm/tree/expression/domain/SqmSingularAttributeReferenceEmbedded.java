/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.persister.common.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;

/**
 * @author Steve Ebersole
 */
public class SqmSingularAttributeReferenceEmbedded extends AbstractSqmSingularAttributeReference implements
		SqmEmbeddableTypedReference {
	public SqmSingularAttributeReferenceEmbedded(
			SqmNavigableSourceReference domainReferenceBinding,
			SingularPersistentAttributeEmbedded boundNavigable) {
		super( domainReferenceBinding, boundNavigable );
	}

	public SqmSingularAttributeReferenceEmbedded(SqmAttributeJoin fromElement) {
		super( fromElement );
	}

	@Override
	public SingularPersistentAttributeEmbedded getReferencedNavigable() {
		return (SingularPersistentAttributeEmbedded) super.getReferencedNavigable();
	}
}
