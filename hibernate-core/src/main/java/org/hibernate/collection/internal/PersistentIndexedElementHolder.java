/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.collection.internal;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Element;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;
import org.hibernate.type.XmlRepresentableType;

/**
 * A persistent wrapper for an XML element
 *
 * @author Gavin King
 */
public abstract class PersistentIndexedElementHolder extends AbstractPersistentCollection {
	protected Element element;
	
	public PersistentIndexedElementHolder(SessionImplementor session, Element element) {
		super(session);
		this.element = element;
		setInitialized();
	}
	
	public static final class IndexedValue {
		String index;
		Object value;
		IndexedValue(String index, Object value) {
			this.index = index;
			this.value = value;
		}
	}
	
	protected static String getIndex(Element element, String indexNodeName, int i) {
		if (indexNodeName!=null) {
			return element.attributeValue(indexNodeName);
		}
		else {
			return Integer.toString(i);
		}
	}
	
	protected static void setIndex(Element element, String indexNodeName, String index) {
		if (indexNodeName!=null) element.addAttribute(indexNodeName, index);
	}
	
	protected static String getIndexAttributeName(CollectionPersister persister) {
		String node = persister.getIndexNodeName();
		return node==null ? null : node.substring(1);
	}
	
	public Serializable getSnapshot(CollectionPersister persister) 
	throws HibernateException {
		
		final Type elementType = persister.getElementType();
		String indexNode = getIndexAttributeName(persister);		
		List elements = element.elements( persister.getElementNodeName() );
		HashMap snapshot = new HashMap( elements.size() );
		for ( int i=0; i<elements.size(); i++ ) {
			Element elem = (Element) elements.get(i);
			Object value = elementType.fromXMLNode( elem, persister.getFactory() );
			Object copy = elementType.deepCopy( value, persister.getFactory() );
			snapshot.put( getIndex(elem, indexNode, i), copy );
		}
		return snapshot;
		
	}

	public Collection getOrphans(Serializable snapshot, String entityName) 
	throws HibernateException {
		//orphan delete not supported for EntityMode.DOM4J
		return Collections.EMPTY_LIST;
	}

	public PersistentIndexedElementHolder(SessionImplementor session, CollectionPersister persister, Serializable key) 
	throws HibernateException {
		super(session);
		Element owner = (Element) session.getPersistenceContext().getCollectionOwner(key, persister);
		if (owner==null) throw new AssertionFailure("null owner");
		//element = XMLHelper.generateDom4jElement( persister.getNodeName() );
		final String nodeName = persister.getNodeName();
		if ( ".".equals(nodeName) ) {
			element = owner;
		}
		else {
			element = owner.element( nodeName );
			if (element==null) element = owner.addElement( nodeName );
		}
	}

	public boolean isWrapper(Object collection) {
		return element==collection;
	}

	public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
		Type elementType = persister.getElementType();
		String indexNode = getIndexAttributeName(persister);		
		HashMap snapshot = (HashMap) getSnapshot();
		List elements = element.elements( persister.getElementNodeName() );
		if ( snapshot.size()!= elements.size() ) return false;
		for ( int i=0; i<snapshot.size(); i++ ) {
			Element elem = (Element) elements.get(i);
			Object old = snapshot.get( getIndex(elem, indexNode, i) );
			Object current = elementType.fromXMLNode( elem, persister.getFactory() );
			if ( elementType.isDirty( old, current, getSession() ) ) return false;
		}
		return true;
	}

	public boolean isSnapshotEmpty(Serializable snapshot) {
		return ( (HashMap) snapshot ).isEmpty();
	}
	
	public boolean empty() {
		return !element.elementIterator().hasNext();
	}

	public Object readFrom(ResultSet rs, CollectionPersister persister, CollectionAliases descriptor, Object owner)
	throws HibernateException, SQLException {
		Object object = persister.readElement( rs, owner, descriptor.getSuffixedElementAliases(), getSession() );
		final Type elementType = persister.getElementType();
		final SessionFactoryImplementor factory = persister.getFactory();
		String indexNode = getIndexAttributeName(persister);

		Element elem = element.addElement( persister.getElementNodeName() );
		elementType.setToXMLNode( elem, object, factory ); 
		
		final Type indexType = persister.getIndexType();
		final Object indexValue = persister.readIndex( rs, descriptor.getSuffixedIndexAliases(), getSession() );
		final String index = ( (XmlRepresentableType) indexType ).toXMLString( indexValue, factory );
		setIndex(elem, indexNode, index);
		return object;
	}

	public Iterator entries(CollectionPersister persister) {
		
		final Type elementType = persister.getElementType();
		String indexNode = getIndexAttributeName(persister);
		List elements =  element.elements( persister.getElementNodeName() );
		int length = elements.size();
		List result = new ArrayList(length);
		for ( int i=0; i<length; i++ ) {
			Element elem = (Element) elements.get(i);
			Object object = elementType.fromXMLNode( elem, persister.getFactory() );
			result.add( new IndexedValue( getIndex(elem, indexNode, i), object ) );
		}
		return result.iterator();
	}

	public void beforeInitialize(CollectionPersister persister, int anticipatedSize) {}

	public boolean isDirectlyAccessible() {
		return true;
	}

	public Object getValue() {
		return element;
	}

	public Iterator getDeletes(CollectionPersister persister, boolean indexIsFormula) 
	throws HibernateException {
		
		final Type indexType = persister.getIndexType();
		HashMap snapshot = (HashMap) getSnapshot();
		HashMap deletes = (HashMap) snapshot.clone();
		deletes.keySet().removeAll( ( (HashMap) getSnapshot(persister) ).keySet() );
		ArrayList deleteList = new ArrayList( deletes.size() );
		for ( Object o : deletes.entrySet() ) {
			Map.Entry me = (Map.Entry) o;
			final Object object = indexIsFormula ?
					me.getValue() :
					( (XmlRepresentableType) indexType ).fromXMLString( (String) me.getKey(), persister.getFactory() );
			if ( object != null ) {
				deleteList.add( object );
			}
		}
		
		return deleteList.iterator();
		
	}

	public boolean needsInserting(Object entry, int i, Type elementType) 
	throws HibernateException {
		HashMap snapshot = (HashMap) getSnapshot();
		IndexedValue iv = (IndexedValue) entry;
		return iv.value!=null && snapshot.get( iv.index )==null;
	}

	public boolean needsUpdating(Object entry, int i, Type elementType) 
	throws HibernateException {
		HashMap snapshot = (HashMap) getSnapshot();
		IndexedValue iv = (IndexedValue) entry;
		Object old = snapshot.get( iv.index );
		return old!=null && elementType.isDirty( old, iv.value, getSession() );
	}

	public Object getIndex(Object entry, int i, CollectionPersister persister) {
		String index = ( (IndexedValue) entry ).index;
		final Type indexType = persister.getIndexType();
		return ( (XmlRepresentableType) indexType ).fromXMLString( index, persister.getFactory() );
	}

	public Object getElement(Object entry) {
		return ( (IndexedValue) entry ).value;
	}

	public Object getSnapshotElement(Object entry, int i) {
		return ( (HashMap) getSnapshot() ).get( ( (IndexedValue) entry ).index );
	}

	public boolean entryExists(Object entry, int i) {
		return entry!=null;
	}

}
