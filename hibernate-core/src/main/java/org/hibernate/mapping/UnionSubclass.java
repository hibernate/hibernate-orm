/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.List;

import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * A mapping model object that represents a subclass in a "union" or
 * {@linkplain jakarta.persistence.InheritanceType#TABLE_PER_CLASS "table per concrete class"}
 * inheritance hierarchy.
 *
 * @author Gavin King
 */
public final class UnionSubclass extends Subclass implements TableOwner {
	private Table table;

	public UnionSubclass(PersistentClass superclass, MetadataBuildingContext buildingContext) {
		super( superclass, buildingContext );
	}

	@Override
	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table = table;
		getSuperclass().addSubclassTable( table );
	}

	public java.util.Set<String> getSynchronizedTables() {
		return synchronizedTables;
	}

	@Override
	protected List<Property> getNonDuplicatedProperties() {
		return getPropertyClosure();
	}

	public Table getIdentityTable() {
		return getTable();
	}

	public Object accept(PersistentClassVisitor mv) {
		return mv.accept(this);
	}
}
