/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection.spi;

import org.hibernate.id.IdentifierGenerator;
import org.hibernate.sqm.domain.BasicType;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeId {
	private final org.hibernate.type.BasicType type;
	private final BasicType sqmType;
	private final IdentifierGenerator generator;

	public PluralAttributeId(
			org.hibernate.type.BasicType type,
			BasicType sqmType,
			IdentifierGenerator generator) {
		this.type = type;
		this.sqmType = sqmType;
		this.generator = generator;
	}

	public org.hibernate.type.BasicType getType() {
		return type;
	}

	public BasicType getSqmType() {
		return sqmType;
	}

	public IdentifierGenerator getGenerator() {
		return generator;
	}
}
