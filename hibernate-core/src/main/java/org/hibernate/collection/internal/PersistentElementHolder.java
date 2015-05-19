/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.collection.internal;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

import org.dom4j.Element;

/**
 * A persistent wrapper for an XML element
 *
 * @author Gavin King
 *
 *
 * @deprecated To be removed in 5.  Removed as part of removing the notion of DOM entity-mode.  See Jira issues
 * <a href="https://hibernate.onjira.com/browse/HHH-7782">HHH-7782</a> and
 * <a href="https://hibernate.onjira.com/browse/HHH-7783">HHH-7783</a> for more information.
 */
@Deprecated
@SuppressWarnings("UnusedDeclaration")
public class PersistentElementHolder extends AbstractPersistentCollection {
	protected Element element;

	/**
	 * Constructs a PersistentElementHolder
	 *
	 * @param session The session
	 * @param element The DOM element
	 */
	@SuppressWarnings("UnusedDeclaration")
	public PersistentElementHolder(SessionImplementor session, Element element) {
		super( session );
		this.element = element;
		setInitialized();
	}

	/**
	 * Constructs a PersistentElementHolder
	 *
	 * @param session The session
	 * @param persister The collection persister
	 * @param key The collection key (the fk value)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public PersistentElementHolder(SessionImplementor session, CollectionPersister persister, Serializable key) {
		super( session );
		final Element owner = (Element) session.getPersistenceContext().getCollectionOwner( key, persister );
		if ( owner == null ) {
			throw new AssertionFailure("null owner");
		}

		final String nodeName = persister.getNodeName();
		if ( ".".equals( nodeName ) ) {
			element = owner;
		}
		else {
			element = owner.element( nodeName );
			if ( element == null ) {
				element = owner.addElement( nodeName );
			}
		}
	}

	@Override
	@SuppressWarnings({"unchecked", "deprecation"})
	public Serializable getSnapshot(CollectionPersister persister) throws HibernateException {
		final Type elementType = persister.getElementType();
		final List subElements = element.elements( persister.getElementNodeName() );
		final ArrayList snapshot = new ArrayList( subElements.size() );
		for ( Object subElement : subElements ) {
			final Element element = (Element) subElement;
			final Object value = elementType.fromXMLNode( element, persister.getFactory() );
			final Object copy = elementType.deepCopy( value, persister.getFactory() );
			snapshot.add( copy );
		}
		return snapshot;

	}

	@Override
	public Collection getOrphans(Serializable snapshot, String entityName) {
		//orphan delete not supported for EntityMode.DOM4J
		return Collections.EMPTY_LIST;
	}

	@Override
	public boolean isWrapper(Object collection) {
		return element == collection;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
		final Type elementType = persister.getElementType();
		final ArrayList snapshot = (ArrayList) getSnapshot();
		final List elements = element.elements( persister.getElementNodeName() );
		if ( snapshot.size() != elements.size() ) {
			return false;
		}
		for ( int i=0; i<snapshot.size(); i++ ) {
			final Object old = snapshot.get( i );
			final Element elem = (Element) elements.get( i );
			final Object current = elementType.fromXMLNode( elem, persister.getFactory() );
			if ( elementType.isDirty( old, current, getSession() ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isSnapshotEmpty(Serializable snapshot) {
		return ( (Collection) snapshot ).isEmpty();
	}

	@Override
	public boolean empty() {
		return !element.elementIterator().hasNext();
	}

	@Override
	@SuppressWarnings("deprecation")
	public Object readFrom(ResultSet rs, CollectionPersister persister, CollectionAliases descriptor, Object owner)
			throws HibernateException, SQLException {
		final Object object = persister.readElement( rs, owner, descriptor.getSuffixedElementAliases(), getSession() );
		final Type elementType = persister.getElementType();
		final Element subElement = element.addElement( persister.getElementNodeName() );
		elementType.setToXMLNode( subElement, object, persister.getFactory() );
		return object;
	}

	@Override
	@SuppressWarnings({"deprecation", "unchecked"})
	public Iterator entries(CollectionPersister persister) {
		final Type elementType = persister.getElementType();
		final List subElements =  element.elements( persister.getElementNodeName() );
		final int length = subElements.size();
		final List result = new ArrayList(length);
		for ( Object subElementO : subElements ) {
			final Element subElement = (Element) subElementO;
			final Object object = elementType.fromXMLNode( subElement, persister.getFactory() );
			result.add( object );
		}
		return result.iterator();
	}

	@Override
	public void beforeInitialize(CollectionPersister persister, int anticipatedSize) {
	}

	@Override
	public boolean isDirectlyAccessible() {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void initializeFromCache(CollectionPersister persister, Serializable disassembled, Object owner)
			throws HibernateException {
		final Type collectionElementType = persister.getElementType();
		final Serializable[] cachedSnapshot = (Serializable[]) disassembled;
		for ( Serializable cachedItem : cachedSnapshot ) {
			final Object object = collectionElementType.assemble( cachedItem, getSession(), owner );
			final Element subElement = element.addElement( persister.getElementNodeName() );
			collectionElementType.setToXMLNode( subElement, object, persister.getFactory() );
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public Serializable disassemble(CollectionPersister persister) throws HibernateException {
		final Type collectionElementType = persister.getElementType();
		final List elements =  element.elements( persister.getElementNodeName() );
		final int length = elements.size();
		final Serializable[] result = new Serializable[length];
		for ( int i=0; i<length; i++ ) {
			final Element elem = (Element) elements.get( i );
			final Object object = collectionElementType.fromXMLNode( elem, persister.getFactory() );
			result[i] = collectionElementType.disassemble( object, getSession(), null );
		}
		return result;
	}

	@Override
	public Object getValue() {
		return element;
	}

	@Override
	@SuppressWarnings({"unchecked", "deprecation"})
	public Iterator getDeletes(CollectionPersister persister, boolean indexIsFormula) throws HibernateException {
		final Type elementType = persister.getElementType();
		final ArrayList snapshot = (ArrayList) getSnapshot();
		final List elements = element.elements( persister.getElementNodeName() );
		final ArrayList result = new ArrayList();
		for ( int i=0; i<snapshot.size(); i++ ) {
			final Object old = snapshot.get( i );
			if ( i >= elements.size() ) {
				result.add( old );
			}
			else {
				final Element elem = (Element) elements.get( i );
				final Object object = elementType.fromXMLNode( elem, persister.getFactory() );
				if ( elementType.isDirty( old, object, getSession() ) ) {
					result.add( old );
				}
			}
		}
		return result.iterator();
	}

	@Override
	public boolean needsInserting(Object entry, int i, Type elementType) throws HibernateException {
		final ArrayList snapshot = (ArrayList) getSnapshot();
		return i >= snapshot.size()
				|| elementType.isDirty( snapshot.get( i ), entry, getSession() );
	}

	@Override
	public boolean needsUpdating(Object entry, int i, Type elementType) throws HibernateException {
		return false;
	}

	@Override
	public Object getIndex(Object entry, int i, CollectionPersister persister) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getElement(Object entry) {
		return entry;
	}

	@Override
	public Object getSnapshotElement(Object entry, int i) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean entryExists(Object entry, int i) {
		return entry!=null;
	}
}
