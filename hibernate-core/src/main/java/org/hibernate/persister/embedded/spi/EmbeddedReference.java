/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.embedded.spi;

import org.hibernate.persister.common.spi.ExpressableType;
import org.hibernate.sqm.domain.SqmExpressableTypeEmbedded;
import org.hibernate.type.spi.EmbeddedType;

/**
 * Describes parts of the domain model that can be composite values.
 *
 * @author Steve Ebersole
 */
public interface EmbeddedReference<J> extends SqmExpressableTypeEmbedded, ExpressableType<J> {
	@Override
	EmbeddedContainer getSource();

	@Override
	EmbeddedType getExportedDomainType();

	EmbeddedPersister getEmbeddablePersister();
}
