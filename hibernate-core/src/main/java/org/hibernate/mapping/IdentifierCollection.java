/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.Mapping;

/**
 * A collection with a synthetic "identifier" column
 */
public abstract class IdentifierCollection extends Collection {

	public static final String DEFAULT_IDENTIFIER_COLUMN_NAME = "id";

	private KeyValue identifier;

	/**
	 * @deprecated Use {@link IdentifierCollection#IdentifierCollection(MetadataBuildingContext, PersistentClass)} instead.
 	 */
	@Deprecated
	public IdentifierCollection(MetadataImplementor metadata, PersistentClass owner) {
		super( metadata, owner );
	}

	public IdentifierCollection(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );
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
		return other instanceof IdentifierCollection
				&& isSame( (IdentifierCollection) other );
	}

	public boolean isSame(IdentifierCollection other) {
		return super.isSame( other )
				&& isSame( identifier, other.identifier );
	}

	void createPrimaryKey() {
		if ( !isOneToMany() ) {
			PrimaryKey pk = new PrimaryKey( getCollectionTable() );
			pk.addColumns( getIdentifier().getColumnIterator() );
			getCollectionTable().setPrimaryKey(pk);
		}
		// create an index on the key columns??
	}

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
}
