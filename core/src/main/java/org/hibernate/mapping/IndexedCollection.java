//$Id: IndexedCollection.java 5797 2005-02-20 08:37:56Z oneovthafew $
package org.hibernate.mapping;

import java.util.Iterator;

import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;

/**
 * Indexed collections include Lists, Maps, arrays and
 * primitive arrays.
 * @author Gavin King
 */
public abstract class IndexedCollection extends Collection {

	public static final String DEFAULT_INDEX_COLUMN_NAME = "idx";

	private Value index;
	private String indexNodeName;

	public IndexedCollection(PersistentClass owner) {
		super(owner);
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

	void createPrimaryKey() {
		if ( !isOneToMany() ) {
			PrimaryKey pk = new PrimaryKey();
			pk.addColumns( getKey().getColumnIterator() );
			
			// index should be last column listed
			boolean isFormula = false;
			Iterator iter = getIndex().getColumnIterator();
			while ( iter.hasNext() ) {
				if ( ( (Selectable) iter.next() ).isFormula() ) isFormula=true;
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
		super.validate(mapping);
		if ( !getIndex().isValid(mapping) ) {
			throw new MappingException(
				"collection index mapping has wrong number of columns: " +
				getRole() +
				" type: " +
				getIndex().getType().getName()
			);
		}
		if ( indexNodeName!=null && !indexNodeName.startsWith("@") ) {
			throw new MappingException("index node must be an attribute: " + indexNodeName );
		}
	}
	
	public boolean isList() {
		return false;
	}

	public String getIndexNodeName() {
		return indexNodeName;
	}

	public void setIndexNodeName(String indexNodeName) {
		this.indexNodeName = indexNodeName;
	}
	

}
