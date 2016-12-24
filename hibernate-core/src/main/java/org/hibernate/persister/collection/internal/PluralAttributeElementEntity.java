/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.collection.internal;

import java.util.List;
import java.util.Optional;

import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.collection.spi.PluralAttributeElement;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.persister.entity.spi.ImprovedEntityPersister;
import org.hibernate.sqm.domain.DomainReference;
import org.hibernate.sqm.domain.EntityReference;
import org.hibernate.type.spi.EntityType;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeElementEntity implements PluralAttributeElement<EntityType> {
	private final CollectionPersister collectionPersister;
	private final EntityPersister elementPersister;
	private final ElementClassification classification;
	private final EntityType type;
	private final List<Column> columns;

	public PluralAttributeElementEntity(
			CollectionPersister collectionPersister,
			EntityPersister elementPersister,
			ElementClassification classification,
			EntityType type,
			List<Column> columns) {
		this.collectionPersister = collectionPersister;
		this.elementPersister = elementPersister;
		this.classification = classification;
		this.type = type;
		this.columns = columns;
	}

	public EntityPersister getElementPersister() {
		return elementPersister;
	}

	@Override
	public ElementClassification getClassification() {
		return classification;
	}

	@Override
	public DomainReference getType() {
		return this;
	}

	@Override
	public EntityType getOrmType() {
		return type;
	}

	public List<Column> getColumns() {
		return columns;
	}

	@Override
	public String asLoggableText() {
		return "PluralAttributeElement(" + collectionPersister.getRole() + " [" + getOrmType().getName() + "])" ;
	}

	@Override
	public Optional<EntityReference> toEntityReference() {
		return Optional.of( elementPersister );
	}
}
