/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

	private DependantBasicValue(DependantBasicValue original) {
		super( original );
		this.referencedValue = original.referencedValue.copy();
		this.nullable = original.nullable;
		this.updateable = original.updateable;
	}

	@Override
	public DependantBasicValue copy() {
		return new DependantBasicValue( this );
	}

	@Override
	protected Resolution<?> buildResolution() {
		return referencedValue.resolve();
	}


}
