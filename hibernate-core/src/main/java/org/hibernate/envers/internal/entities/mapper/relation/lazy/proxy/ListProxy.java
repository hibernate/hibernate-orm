/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation.lazy.proxy;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ListProxy<U> extends CollectionProxy<U, List<U>> implements List<U> {
	private static final long serialVersionUID = -5479232938279790987L;

	public ListProxy() {
	}

	public ListProxy(org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.Initializor<List<U>> initializor) {
		super( initializor );
	}

	@Override
	public boolean addAll(int index, Collection<? extends U> c) {
		checkInit();
		return delegate.addAll( index, c );
	}

	@Override
	public U get(int index) {
		checkInit();
		return delegate.get( index );
	}

	@Override
	public U set(int index, U element) {
		checkInit();
		return delegate.set( index, element );
	}

	@Override
	public void add(int index, U element) {
		checkInit();
		delegate.add( index, element );
	}

	@Override
	public U remove(int index) {
		checkInit();
		return delegate.remove( index );
	}

	@Override
	public int indexOf(Object o) {
		checkInit();
		return delegate.indexOf( o );
	}

	@Override
	public int lastIndexOf(Object o) {
		checkInit();
		return delegate.lastIndexOf( o );
	}

	@Override
	public ListIterator<U> listIterator() {
		checkInit();
		return delegate.listIterator();
	}

	@Override
	public ListIterator<U> listIterator(int index) {
		checkInit();
		return delegate.listIterator( index );
	}

	@Override
	public List<U> subList(int fromIndex, int toIndex) {
		checkInit();
		return delegate.subList( fromIndex, toIndex );
	}
}
