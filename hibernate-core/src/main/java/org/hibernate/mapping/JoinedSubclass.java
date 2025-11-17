/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * A mapping model object that represents a subclass in a "joined" or
 * {@linkplain jakarta.persistence.InheritanceType#JOINED "table per subclass"}
 * inheritance hierarchy.
 *
 * @author Gavin King
 */
public final class JoinedSubclass extends Subclass implements TableOwner {
	private Table table;
	private KeyValue key;

	public JoinedSubclass(PersistentClass superclass, MetadataBuildingContext buildingContext) {
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

	public KeyValue getKey() {
		return key;
	}

	public void setKey(KeyValue key) {
		this.key = key;
	}

	public void validate(Metadata mapping) throws MappingException {
		super.validate( mapping );
		if ( key != null && !key.isValid( mapping ) ) {
			throw new MappingException(
					"subclass key mapping has wrong number of columns: " +
					getEntityName() +
					" type: " +
					key.getType().getName()
				);
		}
	}

	public List<Property> getReferenceableProperties() {
		return getProperties();
	}

	public Object accept(PersistentClassVisitor mv) {
		return mv.accept(this);
	}
}
