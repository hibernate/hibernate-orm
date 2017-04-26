/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.persister.common.spi.SingularPersistentAttribute;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmSingularAttributeReference
		extends AbstractSqmAttributeReference<SingularPersistentAttribute>
		implements SqmSingularAttributeReference {
	public AbstractSqmSingularAttributeReference(
			SqmNavigableSourceReference domainReferenceBinding,
			SingularPersistentAttribute boundNavigable) {
		super( domainReferenceBinding, boundNavigable );
	}

	public AbstractSqmSingularAttributeReference(SqmAttributeJoin fromElement) {
		super( fromElement );
	}
}
