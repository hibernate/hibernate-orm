/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity.internal;

import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.entity.spi.EntityDiscriminator;
import org.hibernate.type.spi.BasicType;

/**
 * Binding of the discriminator in a entity hierarchy
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class EntityDiscriminatorImpl implements EntityDiscriminator {
	private final BasicType type;
	private final Column column;
	private final boolean inserted;
	private final boolean forced;

	public EntityDiscriminatorImpl(BasicType type, Column column, boolean inserted, boolean forced) {
		this.type = type;
		this.column = column;
		this.inserted = inserted;
		this.forced = forced;
	}

	@Override
	public BasicType getType() {
		return type;
	}

	public Column getColumn() {
		return column;
	}

	public boolean isInserted() {
		return inserted;
	}

	public boolean isForced() {
		return forced;
	}

	@Override
	public String toString() {
		return "EntityDiscriminator{column=" + column
				+ ", forced=" + forced
				+ ", inserted=" + inserted + '}';
	}
}
