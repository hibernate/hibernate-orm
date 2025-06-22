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

	void createPrimaryKey() {
		if ( !isOneToMany() ) {
			PrimaryKey pk = new PrimaryKey( getCollectionTable() );
			pk.addColumns( getIdentifier() );
			getCollectionTable().setPrimaryKey(pk);
		}
		// create an index on the key columns??
	}

	public void validate(MappingContext mappingContext) throws MappingException {
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
