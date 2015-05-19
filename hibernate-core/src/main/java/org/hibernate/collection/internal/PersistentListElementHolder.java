/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.collection.internal;

import java.io.Serializable;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.IntegerType;
import org.hibernate.type.Type;

import org.dom4j.Element;

/**
 * Wraps a collection of DOM sub-elements as a List
 *
 * @author Gavin King
 *
 * @deprecated To be removed in 5.  Removed as part of removing the notion of DOM entity-mode.  See Jira issues
 * <a href="https://hibernate.onjira.com/browse/HHH-7782">HHH-7782</a> and
 * <a href="https://hibernate.onjira.com/browse/HHH-7783">HHH-7783</a> for more information.
 */
@Deprecated
@SuppressWarnings({"UnusedDeclaration", "deprecation"})
public class PersistentListElementHolder extends PersistentIndexedElementHolder {

	/**
	 * Constructs a PersistentListElementHolder.
	 *
	 * @param session The session
	 * @param element The owning DOM element
	 */
	@SuppressWarnings("UnusedDeclaration")
	public PersistentListElementHolder(SessionImplementor session, Element element) {
		super( session, element );
	}

	/**
	 * Constructs a PersistentListElementHolder.
	 *
	 * @param session The session
	 * @param persister The collection persister
	 * @param key The collection key (fk value)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public PersistentListElementHolder(SessionImplementor session, CollectionPersister persister, Serializable key) {
		super( session, persister, key );
	}

	@Override
	@SuppressWarnings("deprecation")
	public void initializeFromCache(CollectionPersister persister, Serializable disassembled, Object owner)
			throws HibernateException {
		final Type elementType = persister.getElementType();
		final String indexNodeName = getIndexAttributeName( persister );
		final Serializable[] cached = (Serializable[]) disassembled;
		for ( int i=0; i<cached.length; i++ ) {
			final Object object = elementType.assemble( cached[i], getSession(), owner );
			final Element subelement = element.addElement( persister.getElementNodeName() );
			elementType.setToXMLNode( subelement, object, persister.getFactory() );
			setIndex( subelement, indexNodeName, Integer.toString( i ) );
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public Serializable disassemble(CollectionPersister persister) throws HibernateException {
		final Type elementType = persister.getElementType();
		final String indexNodeName = getIndexAttributeName( persister );
		final List elements =  element.elements( persister.getElementNodeName() );
		final int length = elements.size();
		final Serializable[] result = new Serializable[length];
		for ( int i=0; i<length; i++ ) {
			final Element elem = (Element) elements.get( i );
			final Object object = elementType.fromXMLNode( elem, persister.getFactory() );
			final Integer index = IntegerType.INSTANCE.fromString( getIndex( elem, indexNodeName, i ) );
			result[index] = elementType.disassemble( object, getSession(), null );
		}
		return result;
	}


}
