/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * @author Steve Ebersole
 */
public class DependantBasicValue extends BasicValue {
	private final BasicValue referencedValue;

	private final boolean nullable;
	private final boolean updateable;

	public DependantBasicValue(
			MetadataBuildingContext buildingContext,
			Table table,
			BasicValue referencedValue,
			boolean nullable,
			boolean updateable) {
		super( buildingContext, table );
		this.referencedValue = referencedValue;
		this.nullable = nullable;
		this.updateable = updateable;
	}

	@Override
	protected Resolution<?> buildResolution() {
		return referencedValue.resolve();
	}


}
