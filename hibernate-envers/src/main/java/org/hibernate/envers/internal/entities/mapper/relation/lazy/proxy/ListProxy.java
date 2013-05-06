/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
