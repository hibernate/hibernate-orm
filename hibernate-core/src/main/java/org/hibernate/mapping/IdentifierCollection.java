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
 * A mapping model object representing a collection with a synthetic "identifier" column,
 * that is, a surrogate key.
 */
public non-sealed abstract class IdentifierCollection extends Collection {

	public static final String DEFAULT_IDENTIFIER_COLUMN_NAME = "id";

	private KeyValue identifier;

	public IdentifierCollection(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );
	}

	public IdentifierCollection(
			Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver,
			PersistentClass owner,
			MetadataBuildingContext buildingContext) {
		super( customTypeBeanResolver, owner, buildingContext );
	}

	protected IdentifierCollection(IdentifierCollection original) {
		super( original );
		this.identifier = (KeyValue) original.identifier.copy();
	}

	public KeyValue getIdentifier() {
		return identifier;
	}

	public void setIdentifier(KeyValue identifier) {
		this.identifier = identifier;
		if ( identifier instanceof AppliedMappingPart mappingPart && getRole() != null ) {
			mappingPart.setMappingRole(
					MappingRole.collection( getRole() ).append( MappingRole.PartKind.COLLECTION_IDENTIFIER )
			);
		}
	}

	@Override
	public void setRole(String role) {
		super.setRole( role );
		if ( identifier instanceof AppliedMappingPart mappingPart && role != null ) {
			mappingPart.setMappingRole(
					MappingRole.collection( role ).append( MappingRole.PartKind.COLLECTION_IDENTIFIER )
			);
		}
	}

	@Override
	public void setMappingRole(MappingRole mappingRole) {
		super.setMappingRole( mappingRole );
		if ( identifier instanceof AppliedMappingPart mappingPart ) {
			mappingPart.setMappingRole(
					mappingRole == null ? null : mappingRole.append( MappingRole.PartKind.COLLECTION_IDENTIFIER )
			);
		}
	}

	public final boolean isIdentified() {
		return true;
	}

	@Override
	public boolean isSame(Collection other) {
		return other instanceof IdentifierCollection identifierCollection
			&& isSame( identifierCollection );
	}

	public boolean isSame(IdentifierCollection other) {
		return super.isSame( other )
			&& isSame( identifier, other.identifier );
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

		assert getElement() != null : "IdentifierCollection identifier not bound : " + getRole();

		if ( !getIdentifier().isValid( mappingContext ) ) {
			throw new MappingException(
				"collection id mapping has wrong number of columns: " +
				getRole() +
				" type: " +
				getIdentifier().getType().getName()
			);
		}
	}
}
