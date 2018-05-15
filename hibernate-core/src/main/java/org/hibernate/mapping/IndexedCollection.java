/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.List;

import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.model.relational.MappedPrimaryKey;

/**
 * Indexed collections include Lists, Maps, arrays and
 * primitive arrays.
 * @author Gavin King
 */
public abstract class IndexedCollection extends Collection {

	public static final String DEFAULT_INDEX_COLUMN_NAME = "idx";

	private Value index;

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
			final MappedPrimaryKey pk = new PrimaryKey( getMappedTable() );
			pk.addColumns( getKey().getMappedColumns() );
			// index should be last column listed
			boolean isFormula = false;
			for( MappedColumn selectable : (List<MappedColumn>) getIndex().getMappedColumns() ){
				if(selectable.isFormula()){
					isFormula = true;
				}
			}
			if (isFormula) {
				//if it is a formula index, use the element columns in the PK
				pk.addColumns( getElement().getMappedColumns() );
			}
			else {
				pk.addColumns( getIndex().getMappedColumns() );
			}
			getMappedTable().setPrimaryKey(pk);
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

	public boolean isList() {
		return false;
	}
}
