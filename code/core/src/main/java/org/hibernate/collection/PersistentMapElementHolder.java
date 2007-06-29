//$Id: PersistentMapElementHolder.java 6838 2005-05-20 19:50:07Z oneovthafew $
package org.hibernate.collection;

import java.io.Serializable;
import java.util.List;

import org.dom4j.Element;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.NullableType;
import org.hibernate.type.Type;

/**
 * @author Gavin King
 */
public class PersistentMapElementHolder extends PersistentIndexedElementHolder {

	public PersistentMapElementHolder(SessionImplementor session, Element element) {
		super( session, element );
	}

	public PersistentMapElementHolder(SessionImplementor session, CollectionPersister persister,
			Serializable key) throws HibernateException {
		super( session, persister, key );
	}

	public void initializeFromCache(CollectionPersister persister, Serializable disassembled, Object owner)
	throws HibernateException {
		
		Type elementType = persister.getElementType();
		Type indexType = persister.getIndexType();
		final String indexNodeName = getIndexAttributeName(persister);

		Serializable[] cached = (Serializable[]) disassembled;

		for ( int i=0; i<cached.length; ) {
			Object index = indexType.assemble( cached[i++], getSession(), owner );
			Object object = elementType.assemble( cached[i++], getSession(), owner );
			
			Element subelement = element.addElement( persister.getElementNodeName() );
			elementType.setToXMLNode( subelement, object, persister.getFactory() );
			
			String indexString = ( (NullableType) indexType ).toXMLString( index, persister.getFactory() );
			setIndex( subelement, indexNodeName, indexString );
		}
		
	}

	public Serializable disassemble(CollectionPersister persister) throws HibernateException {
		
		Type elementType = persister.getElementType();
		Type indexType = persister.getIndexType();
		final String indexNodeName = getIndexAttributeName(persister);

		List elements =  element.elements( persister.getElementNodeName() );
		int length = elements.size();
		Serializable[] result = new Serializable[length*2];
		for ( int i=0; i<length*2; ) {
			Element elem = (Element) elements.get(i/2);
			Object object = elementType.fromXMLNode( elem, persister.getFactory() );
			final String indexString = getIndex(elem, indexNodeName, i);
			Object index = ( (NullableType) indexType ).fromXMLString( indexString, persister.getFactory() );
			result[i++] = indexType.disassemble( index, getSession(), null );
			result[i++] = elementType.disassemble( object, getSession(), null );
		}
		return result;
	}


}
