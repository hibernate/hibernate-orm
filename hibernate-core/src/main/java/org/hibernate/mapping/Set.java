/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.CollectionType;
import org.hibernate.type.OrderedSetType;
import org.hibernate.type.SetType;
import org.hibernate.type.SortedSetType;
import org.hibernate.usertype.UserCollectionType;

/**
 * A mapping model object representing a collection of type {@link java.util.List}.
 * A set has no nullable element columns (unless it is a one-to-many association).
 * It has a primary key consisting of all columns (i.e. key columns + element columns),
 * or a unique key if some element columns are nullable.
 *
 * @author Gavin King
 */
public non-sealed class Set extends Collection {
	/**
	 * Used by hbm.xml binding
	 */
	public Set(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );
	}

	/**
	 * Used by annotation binding
	 */
	public Set(Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver, PersistentClass persistentClass, MetadataBuildingContext buildingContext) {
		super( customTypeBeanResolver, persistentClass, buildingContext );
	}

	private Set(Set original) {
		super( original );
	}

	@Override
	public Set copy() {
		return new Set( this );
	}

	public void validate(Metadata mappingContext) throws MappingException {
		super.validate( mappingContext );
		//for backward compatibility, disable this:
		/*Iterator iter = getElement().getColumnIterator();
		while ( iter.hasNext() ) {
			Column col = (Column) iter.next();
			if ( !col.isNullable() ) {
				return;
			}
		}
		throw new MappingException("set element mappings must have at least one non-nullable column: " + getRole() );*/
	}

	public boolean isSet() {
		return true;
	}

	public CollectionType getDefaultCollectionType() {
		if ( isSorted() ) {
			return new SortedSetType( getRole(), getReferencedPropertyName(), getComparator() );
		}
		else if ( hasOrder() ) {
			return new OrderedSetType( getRole(), getReferencedPropertyName() );
		}
		else {
			return new SetType( getRole(), getReferencedPropertyName() );
		}
	}

	/**
	 * Compatibility-only implementation of the hidden collection key hook.
	 *
	 * @deprecated ORM boot code should use
	 * {@link org.hibernate.boot.mapping.internal.materialize.CollectionKeyMappingMaterializer}
	 * with an explicit resolved collection-table key product instead.
	 */
	@Override
	@Deprecated(since = "9.0", forRemoval = true)
	void createPrimaryKey() {
		throw new UnsupportedOperationException(
				"Set primary-key materialization requires CollectionKeyMappingMaterializer"
		);
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
}
