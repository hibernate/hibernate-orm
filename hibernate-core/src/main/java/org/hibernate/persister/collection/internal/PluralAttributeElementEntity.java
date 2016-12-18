/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection.internal;

import org.hibernate.persister.common.spi.Column;
import org.hibernate.type.spi.EntityType;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeElementEntity implements PluralAttributeElement<EntityType, EntityType> {
	private final ElementClassification classification;
	private final EntityType type;
	private final EntityType sqmType;
	private final Column[] columns;

	public PluralAttributeElementEntity(
			ElementClassification classification,
			EntityType type,
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
	public EntityType getOrmType() {
		return type;
	}

	public Column[] getColumns() {
		return columns;
	}
}
