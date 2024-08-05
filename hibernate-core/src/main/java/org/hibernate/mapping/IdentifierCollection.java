/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserCollectionType;

/**
 * A mapping model object representing a collection with a synthetic "identifier" column,
 * that is, a surrogate key.
 */
public abstract class IdentifierCollection extends Collection {

	public static final String DEFAULT_IDENTIFIER_COLUMN_NAME = "id";

	private KeyValue identifier;

	public IdentifierCollection(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );
	}

	public IdentifierCollection(Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver, PersistentClass owner, MetadataBuildingContext buildingContext) {
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

	@Override
	public final boolean isIdentified() {
		return true;
	}

	@Override
	public boolean isSame(Collection other) {
		return other instanceof IdentifierCollection
				&& isSame( (IdentifierCollection) other );
	}

	public boolean isSame(IdentifierCollection other) {
		return super.isSame( other )
				&& isSame( identifier, other.identifier );
	}

	@Override
	void createPrimaryKey() {
		if ( !isOneToMany() ) {
			PrimaryKey pk = new PrimaryKey( getCollectionTable() );
			pk.addColumns( getIdentifier() );
			getCollectionTable().setPrimaryKey(pk);
		}
		// create an index on the key columns??
	}


	@Override
	public void validate(Mapping mapping) throws MappingException {
		super.validate( mapping );

		assert getElement() != null : "IdentifierCollection identifier not bound : " + getRole();

		if ( !getIdentifier().isValid(mapping) ) {
			throw new MappingException(
				"collection id mapping has wrong number of columns: " +
				getRole() +
				" type: " +
				getIdentifier().getType().getName()
			);
		}
	}

	@Override
	public void validate(TypeConfiguration typeConfiguration) throws MappingException {
		super.validate( typeConfiguration );

		assert getElement() != null : "IdentifierCollection identifier not bound : " + getRole();

		if ( !getIdentifier().isValid( typeConfiguration ) ) {
			throw new MappingException(
					"collection id mapping has wrong number of columns: " +
							getRole() +
							" type: " +
							getIdentifier().getType().getName()
			);
		}
	}
}
