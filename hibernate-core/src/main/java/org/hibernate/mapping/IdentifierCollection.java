/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.model.relational.MappedPrimaryKey;

/**
 * A collection with a synthetic "identifier" column
 */
public abstract class IdentifierCollection extends Collection {

	public static final String DEFAULT_IDENTIFIER_COLUMN_NAME = "id";

	private KeyValue identifier;

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
			MappedPrimaryKey pk = new PrimaryKey( getMappedTable() );
			pk.addColumns( getIdentifier().getMappedColumns() );
			getMappedTable().setPrimaryKey(pk);
		}
		// create an index on the key columns??
	}

	public void validate() throws MappingException {
		super.validate();

		assert getElement() != null : "IdentifierCollection identifier not bound : " + getRole();

		if ( !getIdentifier().isValid() ) {
			throw new MappingException(
				"collection id mapping has wrong number of columns: " +
				getRole() +
				" type: " +
				getIdentifier().getJavaTypeMapping().getTypeName()
			);
		}
	}
}
