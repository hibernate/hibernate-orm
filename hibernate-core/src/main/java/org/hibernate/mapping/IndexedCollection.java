/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.Iterator;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.Mapping;

/**
 * Indexed collections include Lists, Maps, arrays and
 * primitive arrays.
 * @author Gavin King
 */
public abstract class IndexedCollection extends Collection {

	public static final String DEFAULT_INDEX_COLUMN_NAME = "idx";

	private Value index;

	/**
	 * @deprecated Use {@link IndexedCollection#IndexedCollection(MetadataBuildingContext, PersistentClass)} insetad.
	 */
	@Deprecated
	public IndexedCollection(MetadataImplementor metadata, PersistentClass owner) {
		super( metadata, owner );
	}

	public IndexedCollection(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );
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

	@Override
	public boolean isSame(Collection other) {
		return other instanceof IndexedCollection
				&& isSame( (IndexedCollection) other );
	}

	public boolean isSame(IndexedCollection other) {
		return super.isSame( other )
				&& isSame( index, other.index );
	}

	void createPrimaryKey() {
		if ( !isOneToMany() ) {
			PrimaryKey pk = new PrimaryKey( getCollectionTable() );
			pk.addColumns( getKey().getColumnIterator() );

			// index should be last column listed
			boolean isFormula = false;
			Iterator iter = getIndex().getColumnIterator();
			while ( iter.hasNext() ) {
				if ( ( (Selectable) iter.next() ).isFormula() ) {
					isFormula=true;
				}
			}
			if (isFormula) {
				//if it is a formula index, use the element columns in the PK
				pk.addColumns( getElement().getColumnIterator() );
			}
			else {
				pk.addColumns( getIndex().getColumnIterator() );
			}
			getCollectionTable().setPrimaryKey(pk);
		}
		else {
			// don't create a unique key, 'cos some
			// databases don't like a UK on nullable
			// columns
			/*ArrayList list = new ArrayList();
			list.addAll( getKey().getConstraintColumns() );
			list.addAll( getIndex().getConstraintColumns() );
			getCollectionTable().createUniqueKey(list);*/
		}
	}

	public void validate(Mapping mapping) throws MappingException {
		super.validate( mapping );

		assert getElement() != null : "IndexedCollection index not bound : " + getRole();

		if ( !getIndex().isValid(mapping) ) {
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
