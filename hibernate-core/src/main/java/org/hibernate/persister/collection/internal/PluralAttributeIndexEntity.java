/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection.internal;

import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.AbstractPluralAttributeIndex;
import org.hibernate.type.spi.EntityType;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeIndexEntity extends AbstractPluralAttributeIndex<EntityType, org.hibernate.sqm.domain.EntityReference> {
	public PluralAttributeIndexEntity(
			EntityType ormType,
			org.hibernate.sqm.domain.EntityReference sqmType,
			Column[] columns) {
		super( ormType, sqmType, columns );
	}
}
