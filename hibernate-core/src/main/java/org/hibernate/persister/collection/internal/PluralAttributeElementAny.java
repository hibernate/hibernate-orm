/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection.internal;

import org.hibernate.persister.collection.spi.PluralAttributeElement;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.sqm.domain.AnyType;
import org.hibernate.sqm.domain.PluralAttribute;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeElementAny implements PluralAttributeElement<org.hibernate.type.AnyType, AnyType> {
	private final org.hibernate.type.AnyType type;
	private final AnyType sqmType;
	private final Column[] columns;

	public PluralAttributeElementAny(org.hibernate.type.AnyType type, AnyType sqmType, Column[] columns) {
		this.type = type;
		this.sqmType = sqmType;
		this.columns = columns;
	}

	@Override
	public PluralAttribute.ElementClassification getElementClassification() {
		return PluralAttribute.ElementClassification.ANY;
	}

	@Override
	public AnyType getSqmType() {
		return sqmType;
	}

	@Override
	public org.hibernate.type.AnyType getOrmType() {
		return type;
	}

	public Column[] getColumns() {
		return columns;
	}
}
