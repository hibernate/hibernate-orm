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
		if ( index instanceof AppliedMappingPart mappingPart && getRole() != null ) {
			mappingPart.setMappingRole( MappingRole.collection( getRole() ).append( MappingRole.PartKind.INDEX ) );
		}
	}

	@Override
	public void setRole(String role) {
		super.setRole( role );
		if ( index instanceof AppliedMappingPart mappingPart && role != null ) {
			mappingPart.setMappingRole( MappingRole.collection( role ).append( MappingRole.PartKind.INDEX ) );
		}
	}

	@Override
	public void setMappingRole(MappingRole mappingRole) {
		super.setMappingRole( mappingRole );
		if ( index instanceof AppliedMappingPart mappingPart ) {
			mappingPart.setMappingRole(
					mappingRole == null ? null : mappingRole.append( MappingRole.PartKind.INDEX )
			);
		}
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
				"Collection primary-key materialization requires CollectionKeyMappingMaterializer"
		);
	}

	public void validate(Metadata mappingContext) throws MappingException {
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
