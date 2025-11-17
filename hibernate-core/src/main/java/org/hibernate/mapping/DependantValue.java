/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.Type;

/**
 * A mapping model object representing a {@linkplain Value value} which is "typed" by reference
 * to some other value (for example, a foreign key is typed by the referenced primary key).
 *
 * @author Gavin King
 */
public class DependantValue extends SimpleValue implements Resolvable, SortableValue {
	private final KeyValue wrappedValue;
	private boolean nullable;
	private boolean updateable;
	private boolean sorted;

	public DependantValue(MetadataBuildingContext buildingContext, Table table, KeyValue prototype) {
		super( buildingContext, table );
		this.wrappedValue = prototype;
	}

	private DependantValue(DependantValue original) {
		super( original );
		this.wrappedValue = (KeyValue) original.wrappedValue.copy();
		this.nullable = original.nullable;
		this.updateable = original.updateable;
		this.sorted = original.sorted;
	}

	@Override
	public DependantValue copy() {
		return new DependantValue( this );
	}

	public KeyValue getWrappedValue() {
		return wrappedValue;
	}

	public Type getType() throws MappingException {
		return wrappedValue.getType();
	}

	@Override
	public void setTypeUsingReflection(String className, String propertyName) {}

	@Override
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	@Override
	public boolean isNullable() {
		return nullable;

	}

	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}

	@Override
	public boolean isUpdateable() {
		return updateable;
	}

	public void setUpdateable(boolean updateable) {
		this.updateable = updateable;
	}

	@Override
	public boolean isSame(SimpleValue other) {
		return other instanceof DependantValue dependantValue
			&& isSame( dependantValue );
	}

	public boolean isSame(DependantValue other) {
		return super.isSame( other )
			&& isSame( wrappedValue, other.wrappedValue );
	}

	@Override
	public boolean resolve(MetadataBuildingContext buildingContext) {
		resolve();
		return true;
	}

	@Override
	public BasicValue.Resolution<?> resolve() {
		if ( wrappedValue instanceof BasicValue basicValue ) {
			return basicValue.resolve();
		}
		// not sure it is ever possible
		throw new UnsupportedOperationException("Trying to resolve the wrapped value but it is non a BasicValue");
	}

	@Override
	public boolean isSorted() {
		return sorted;
	}

	public void setSorted(boolean sorted) {
		this.sorted = sorted;
	}

	@Override
	public int[] sortProperties() {
		if ( !sorted ) {
			sorted = true;
			if ( wrappedValue instanceof SortableValue sortableValue ) {
				final int[] originalOrder = sortableValue.sortProperties();
				if ( originalOrder != null ) {
					sortColumns( originalOrder );
				}
			}
		}
		return null;
	}
}
