/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.collection.spi.CollectionSemantics;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSetSemantics<S extends Set<?>> implements CollectionSemantics<S> {
	@Override
	public Iterator getElementIterator(Set rawCollection) {
		if ( rawCollection == null ) {
			return null;
		}
		return rawCollection.iterator();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitElements(S rawCollection, Consumer action) {
		if ( rawCollection != null ) {
			rawCollection.forEach( action );
		}
	}
}
