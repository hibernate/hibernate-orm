//$Id: PersistentListElementHolder.java 6838 2005-05-20 19:50:07Z oneovthafew $
package org.hibernate.collection;

import java.io.Serializable;
import java.util.List;

import org.dom4j.Element;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

/**
 * @author Gavin King
 */
public class PersistentListElementHolder extends PersistentIndexedElementHolder {

	public PersistentListElementHolder(SessionImplementor session, Element element) {
		super( session, element );
	}

	public PersistentListElementHolder(SessionImplementor session, CollectionPersister persister,
			Serializable key) throws HibernateException {
		super( session, persister, key );
	}

	public void initializeFromCache(CollectionPersister persister, Serializable disassembled, Object owner)
	throws HibernateException {
		
		Type elementType = persister.getElementType();
		final String indexNodeName = getIndexAttributeName(persister);
		Serializable[] cached = (Serializable[]) disassembled;
		for ( int i=0; i<cached.length; i++ ) {
			Object object = elementType.assemble( cached[i], getSession(), owner );
			Element subelement = element.addElement( persister.getElementNodeName() );
			elementType.setToXMLNode( subelement, object, persister.getFactory() );
			setIndex( subelement, indexNodeName, Integer.toString(i) );
		}
		
	}

	public Serializable disassemble(CollectionPersister persister) throws HibernateException {
				
		Type elementType = persister.getElementType();
		final String indexNodeName = getIndexAttributeName(persister);
		List elements =  element.elements( persister.getElementNodeName() );
		int length = elements.size();
		Serializable[] result = new Serializable[length];
		for ( int i=0; i<length; i++ ) {
			Element elem = (Element) elements.get(i);
			Object object = elementType.fromXMLNode( elem, persister.getFactory() );
			Integer index = (Integer) Hibernate.INTEGER.fromStringValue( getIndex(elem, indexNodeName, i) );
			result[ index.intValue() ] = elementType.disassemble( object, getSession(), null );
		}
		return result;
	}


}
