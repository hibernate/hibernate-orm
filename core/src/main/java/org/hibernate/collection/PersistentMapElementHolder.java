/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
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
