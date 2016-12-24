/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.spi;

import org.hibernate.persister.collection.spi.PluralAttributeElement;
import org.hibernate.persister.collection.spi.PluralAttributeId;
import org.hibernate.persister.collection.spi.PluralAttributeIndex;
import org.hibernate.persister.collection.spi.PluralAttributeKey;
import org.hibernate.sqm.domain.PluralAttributeReference;
import org.hibernate.type.CollectionType;

/**
 * @author Steve Ebersole
 */
public interface PluralAttribute extends Attribute, JoinableAttribute, PluralAttributeReference, OrmTypeExporter {
	@Override
	CollectionType getOrmType();

	PluralAttributeKey getForeignKeyDescriptor();
	PluralAttributeId getIdDescriptor();

	@Override
	PluralAttributeElement getElementReference();

	@Override
	PluralAttributeIndex getIndexReference();
}
