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
import org.hibernate.sqm.domain.DomainReference;
import org.hibernate.sqm.domain.EntityReference;
import org.hibernate.type.AnyType;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeElementAny implements PluralAttributeElement<AnyType> {
	private final CollectionPersister collectionPersister;
	private final AnyType type;
	private final List<Column> columns;

	public PluralAttributeElementAny(
			CollectionPersister collectionPersister,
			AnyType type,
			List<Column> columns) {
		this.collectionPersister = collectionPersister;
		this.type = type;
		this.columns = columns;
	}

	@Override
	public ElementClassification getClassification() {
		return ElementClassification.ANY;
	}

	@Override
	public AnyType getOrmType() {
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
	public DomainReference getType() {
		return this;
	}

	@Override
	public Optional<EntityReference> toEntityReference() {
		return Optional.empty();
	}
}
