/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.collection.spi;

import org.hibernate.persister.common.spi.ConvertibleNavigable;
import org.hibernate.type.spi.BasicType;
import org.hibernate.sqm.domain.SqmPluralAttributeElementBasic;

/**
 * @author Steve Ebersole
 */
public interface CollectionElementBasic<J>
		extends CollectionElement<J,BasicType<J>>, ConvertibleNavigable<J>, SqmPluralAttributeElementBasic {
	@Override
	BasicType<J> getExportedDomainType();
}
