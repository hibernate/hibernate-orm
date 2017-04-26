/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.query.sqm.domain.type.SqmDomainTypeEmbeddable;
import org.hibernate.query.sqm.tree.from.SqmFromExporter;

/**
 * @author Steve Ebersole
 */
public interface SqmEmbeddableTypedReference extends SqmNavigableSourceReference, SqmFromExporter {
	@Override
	Navigable getReferencedNavigable();

	@Override
	default SqmDomainTypeEmbeddable getExportedDomainType() {
		return getReferencedNavigable().getExportedDomainType();
	}
}
