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
import org.hibernate.persister.collection.spi.ImprovedCollectionPersister;
import org.hibernate.persister.collection.spi.PluralAttributeElement;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.ConvertibleDomainReference;
import org.hibernate.sqm.domain.DomainReference;
import org.hibernate.sqm.domain.EntityReference;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeElementBasic implements PluralAttributeElement<BasicType>, ConvertibleDomainReference {
	private final CollectionPersister persister;
	private final BasicType type;
	private final List<Column> columns;
	private final AttributeConverter attributeConverter;

	public PluralAttributeElementBasic(
			CollectionPersister persister,
			BasicType type,
			AttributeConverter attributeConverter,
			List<Column> columns) {
		this.persister = persister;
		this.type = type;
		this.attributeConverter = attributeConverter;
		this.columns = columns;
	}

	@Override
	public ElementClassification getClassification() {
		return ElementClassification.BASIC;
	}

	@Override
	public DomainReference getType() {
		return this;
	}

	@Override
	public BasicType getOrmType() {
		return type;
	}

	@Override
	public List<Column> getColumns() {
		return columns;
	}

	@Override
	public String asLoggableText() {
		return "PluralAttributeElement(" + persister.getRole() + " [" + getOrmType().getName() + "])" ;
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
