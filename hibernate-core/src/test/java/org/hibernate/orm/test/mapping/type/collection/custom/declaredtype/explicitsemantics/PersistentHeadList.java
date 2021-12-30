/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.collection.custom.declaredtype.explicitsemantics;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public class PersistentHeadList extends PersistentSet implements IHeadSetList {

	public PersistentHeadList(SharedSessionContractImplementor session) {
		super( session );
	}

	public PersistentHeadList(SharedSessionContractImplementor session, IHeadSetList list) {
		super( session, list );
	}

	@Override
	public Object head() {
		return ( (IHeadSetList) set ).head();
	}

	@Override
	public boolean addAll(int index, Collection c) {
		return set.addAll( c );
	}

	@Override
	public Object get(int index) {
		Iterator iterator = iterator();
		Object next = null;
		for ( int i = 0; i <= index; i++ ) {
			next = iterator.next();
		}
		return next;
	}

	@Override
	public Object set(int index, Object element) {
		remove( index );
		return add( element );
	}

	@Override
	public void add(int index, Object element) {
		add( element );
	}

	@Override
	public Object remove(int index) {
		return remove( get( index ) );
	}

	@Override
	public int indexOf(Object o) {
		throw new UnsupportedOperationException( "Toy class" );
	}

	@Override
	public int lastIndexOf(Object o) {
		throw new UnsupportedOperationException( "Toy class" );
	}

	@Override
	public ListIterator listIterator() {
		throw new UnsupportedOperationException( "Toy class" );
	}

	@Override
	public ListIterator listIterator(int index) {
		throw new UnsupportedOperationException( "Toy class" );
	}

	@Override
	public List subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException( "Toy class" );
	}
}
