/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection.internal;

import org.hibernate.persister.collection.spi.PluralAttributeElement;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.sqm.domain.BasicType;
import org.hibernate.sqm.domain.PluralAttributeElementReference;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeElementBasic implements PluralAttributeElement<org.hibernate.type.spi.BasicType, BasicType> {
	private final org.hibernate.type.spi.BasicType type;
	private final BasicType sqmType;
	private final Column[] columns;

	public PluralAttributeElementBasic(org.hibernate.type.spi.BasicType type, BasicType sqmType, Column[] columns) {
		this.type = type;
		this.sqmType = sqmType;
		this.columns = columns;
	}

	@Override
	public PluralAttributeElementReference.ElementClassification getElementClassification() {
		return PluralAttributeElementReference.ElementClassification.BASIC;
	}

	@Override
	public BasicType getSqmType() {
		return sqmType;
	}

	@Override
	public org.hibernate.type.spi.BasicType getOrmType() {
		return type;
	}

	public Column[] getColumns() {
		return columns;
	}
}
