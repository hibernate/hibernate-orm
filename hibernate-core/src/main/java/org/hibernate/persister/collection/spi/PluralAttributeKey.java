/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection.spi;

import org.hibernate.persister.common.spi.Column;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeKey {
	private final Type type;
	private final org.hibernate.sqm.domain.Type sqmType;
	private final Column[] foreignKeyColumns;
	// todo : referenced values?

	public PluralAttributeKey(
			Type type,
			org.hibernate.sqm.domain.Type sqmType,
			Column[] foreignKeyValues) {
		this.type = type;
		this.sqmType = sqmType;
		this.foreignKeyColumns = foreignKeyValues;
	}

	public Type getType() {
		return type;
	}

	public org.hibernate.sqm.domain.Type getSqmType() {
		return sqmType;
	}

	public Column[] getForeignKeyColumns() {
		return foreignKeyColumns;
	}
}
