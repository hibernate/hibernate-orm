/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.collection.internal;

import java.util.List;
import java.util.Optional;

import javax.persistence.AttributeConverter;

import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.spi.AbstractPluralAttributeIndex;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.ConvertibleDomainReference;
import org.hibernate.sqm.domain.DomainReference;
import org.hibernate.sqm.domain.EntityReference;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeIndexBasic
		extends AbstractPluralAttributeIndex<BasicType>
		implements ConvertibleDomainReference {
	private final AttributeConverter attributeConverter;

	public PluralAttributeIndexBasic(
			CollectionPersister persister,
			BasicType ormType,
			AttributeConverter attributeConverter,
			List<Column> columns) {
		super( persister, ormType, columns );
		this.attributeConverter = attributeConverter;
	}

	@Override
	public IndexClassification getClassification() {
		return IndexClassification.BASIC;
	}

	@Override
	public DomainReference getType() {
		return this;
	}

	@Override
	public Optional<EntityReference> toEntityReference() {
		return Optional.empty();
	}

	@Override
	public Optional<AttributeConverter> getAttributeConverter() {
		return Optional.of( attributeConverter );
	}
}
