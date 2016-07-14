/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection.internal;

import org.hibernate.persister.collection.spi.PluralAttributeElement;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.sqm.domain.EntityType;
import org.hibernate.sqm.domain.PluralAttribute.ElementClassification;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeElementEntity implements PluralAttributeElement<org.hibernate.type.EntityType, EntityType> {
	private final ElementClassification classification;
	private final org.hibernate.type.EntityType type;
	private final EntityType sqmType;
	private final Column[] columns;

	public PluralAttributeElementEntity(
			ElementClassification classification,
			org.hibernate.type.EntityType type,
			EntityType sqmType,
			Column[] columns) {
		this.classification = classification;
		this.type = type;
		this.sqmType = sqmType;
		this.columns = columns;
	}

	@Override
	public ElementClassification getElementClassification() {
		return classification;
	}

	@Override
	public EntityType getSqmType() {
		return sqmType;
	}

	@Override
	public org.hibernate.type.EntityType getOrmType() {
		return type;
	}

	public Column[] getColumns() {
		return columns;
	}
}
