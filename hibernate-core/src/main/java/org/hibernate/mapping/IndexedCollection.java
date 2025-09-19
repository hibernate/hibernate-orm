/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.MappingContext;
import org.hibernate.usertype.UserCollectionType;

/**
 * Indexed collections include Lists, Maps, arrays and
 * primitive arrays.
 * @author Gavin King
 */
public sealed abstract class IndexedCollection extends Collection permits Map, List  {

	public static final String DEFAULT_INDEX_COLUMN_NAME = "idx";

	private Value index;

	public IndexedCollection(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );
	}

	public IndexedCollection(Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver, PersistentClass owner, MetadataBuildingContext buildingContext) {
		super( customTypeBeanResolver, owner, buildingContext );
	}

	protected IndexedCollection(IndexedCollection original) {
		super( original );
		this.index = original.index == null ? null : original.index.copy();
	}

	public Value getIndex() {
		return index;
	}

	public void setIndex(Value index) {
		this.index = index;
	}

	public final boolean isIndexed() {
		return true;
	}

	public boolean hasMapKeyProperty() {
		return false;
	}

	@Override
	public boolean isSame(Collection other) {
		return other instanceof IndexedCollection indexedCollection
			&& isSame( indexedCollection );
	}

	public boolean isSame(IndexedCollection other) {
		return super.isSame( other )
			&& isSame( index, other.index );
	}

	void createPrimaryKey() {
		if ( !isOneToMany() ) {
			final var primaryKey = new PrimaryKey( getCollectionTable() );
			primaryKey.addColumns( getKey() );

			// index should be last column listed
			boolean indexIsPartOfElement = false;
			for ( var selectable: getIndex().getSelectables() ) {
				if ( selectable.isFormula() || !getCollectionTable().containsColumn( (Column) selectable ) ) {
					indexIsPartOfElement = true;
				}
			}
			if ( indexIsPartOfElement ) {
				//if it is part of the element, use the element columns in the PK
				primaryKey.addColumns( getElement() );
			}
			else {
				primaryKey.addColumns( getIndex() );
			}
			getCollectionTable().setPrimaryKey( primaryKey );
		}
//		else {
			// don't create a unique key, 'cos some
			// databases don't like a UK on nullable
			// columns
			/*ArrayList list = new ArrayList();
			list.addAll( getKey().getConstraintColumns() );
			list.addAll( getIndex().getConstraintColumns() );
			getCollectionTable().createUniqueKey(list);*/
//		}
	}

	public void validate(MappingContext mappingContext) throws MappingException {
		super.validate( mappingContext );

		assert getElement() != null : "IndexedCollection index not bound : " + getRole();

		if ( !getIndex().isValid( mappingContext ) ) {
			throw new MappingException(
					"collection index mapping has wrong number of columns: " +
							getRole() +
							" type: " +
							getIndex().getType().getName()
			);
		}
	}


	public boolean isList() {
		return false;
	}
}
