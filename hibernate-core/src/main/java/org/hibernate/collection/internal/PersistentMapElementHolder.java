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
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;
import org.hibernate.type.XmlRepresentableType;

import org.dom4j.Element;

/**
 * Wraps a collection of DOM sub-elements as a Map
 *
 * @author Gavin King
 *
 * @deprecated To be removed in 5.  Removed as part of removing the notion of DOM entity-mode.  See Jira issues
 * <a href="https://hibernate.onjira.com/browse/HHH-7782">HHH-7782</a> and
 * <a href="https://hibernate.onjira.com/browse/HHH-7783">HHH-7783</a> for more information.
 */
@SuppressWarnings({"UnusedDeclaration", "deprecation"})
@Deprecated
public class PersistentMapElementHolder extends PersistentIndexedElementHolder {

	/**
	 * Constructs a PersistentMapElementHolder.
	 *
	 * @param session The session
	 * @param element The owning DOM element
	 */
	public PersistentMapElementHolder(SessionImplementor session, Element element) {
		super( session, element );
	}

	/**
	 * Constructs a PersistentMapElementHolder.
	 *
	 * @param session The session
	 * @param persister The collection persister
	 * @param key The collection key (fk value)
	 */
	public PersistentMapElementHolder(SessionImplementor session, CollectionPersister persister, Serializable key) {
		super( session, persister, key );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initializeFromCache(CollectionPersister persister, Serializable disassembled, Object owner) {
		final Type elementType = persister.getElementType();
		final Type indexType = persister.getIndexType();
		final String indexNodeName = getIndexAttributeName( persister );

		final Serializable[] cached = (Serializable[]) disassembled;
		int i = 0;
		while ( i < cached.length ) {
			final Object index = indexType.assemble( cached[i++], getSession(), owner );
			final Object object = elementType.assemble( cached[i++], getSession(), owner );

			final Element subElement = element.addElement( persister.getElementNodeName() );
			elementType.setToXMLNode( subElement, object, persister.getFactory() );

			final String indexString = ( (XmlRepresentableType) indexType ).toXMLString( index, persister.getFactory() );
			setIndex( subElement, indexNodeName, indexString );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Serializable disassemble(CollectionPersister persister) throws HibernateException {
		final Type elementType = persister.getElementType();
		final Type indexType = persister.getIndexType();
		final String indexNodeName = getIndexAttributeName( persister );

		final List elements =  element.elements( persister.getElementNodeName() );
		final int length = elements.size();
		final Serializable[] result = new Serializable[length*2];
		int i = 0;
		while ( i < length*2 ) {
			final Element elem = (Element) elements.get( i/2 );
			final Object object = elementType.fromXMLNode( elem, persister.getFactory() );
			final String indexString = getIndex( elem, indexNodeName, i );
			final Object index = ( (XmlRepresentableType) indexType ).fromXMLString( indexString, persister.getFactory() );
			result[i++] = indexType.disassemble( index, getSession(), null );
			result[i++] = elementType.disassemble( object, getSession(), null );
		}
		return result;
	}
}
