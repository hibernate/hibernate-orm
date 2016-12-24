/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.spi;

import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSingularAttribute<O extends Type>
		extends AbstractAttribute
		implements SingularAttribute {
	private final O ormType;
	private final boolean nullable;

	public AbstractSingularAttribute(
			AttributeContainer attributeContainer,
			String name,
			O ormType,
			boolean nullable) {
		super( attributeContainer, name );
		this.ormType = ormType;
		this.nullable = nullable;
	}

	@Override
	public O getOrmType() {
		return ormType;
	}

	@Override
	public boolean isNullable() {
		return nullable;
	}
}
