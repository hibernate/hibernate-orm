/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.CollectionType;
import org.hibernate.type.OrderedSetType;
import org.hibernate.type.SetType;
import org.hibernate.type.SortedSetType;
import org.hibernate.type.MappingContext;
import org.hibernate.usertype.UserCollectionType;

/**
 * A mapping model object representing a collection of type {@link java.util.List}.
 * A set has no nullable element columns (unless it is a one-to-many association).
 * It has a primary key consisting of all columns (i.e. key columns + element columns).
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

	private Set(Collection original) {
		super( original );
	}

	@Override
	public Set copy() {
		return new Set( this );
	}

	public void validate(MappingContext mappingContext) throws MappingException {
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

	void createPrimaryKey() {
		if ( !isOneToMany() ) {
			final var collectionTable = getCollectionTable();
			var primaryKey = collectionTable.getPrimaryKey();
			if ( primaryKey == null ) {
				primaryKey = new PrimaryKey( getCollectionTable() );
				primaryKey.addColumns( getKey() );
				for ( var selectable : getElement().getSelectables() ) {
					if ( selectable instanceof Column col ) {
						if ( !col.isNullable() ) {
							primaryKey.addColumn( col );
						}
						else {
							return;
						}
					}
				}
				if ( primaryKey.getColumnSpan() != getKey().getColumnSpan() ) {
					collectionTable.setPrimaryKey( primaryKey );
				}
//				else {
					//for backward compatibility, allow a set with no not-null
					//element columns, using all columns in the row locator SQL
					//TODO: create an implicit not null constraint on all cols?
//				}
			}
		}
//		else {
			//create an index on the key columns??
//		}
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
}
