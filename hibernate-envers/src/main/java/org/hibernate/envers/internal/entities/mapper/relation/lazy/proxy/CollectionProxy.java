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

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

import org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.Initializor;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class CollectionProxy<U, T extends Collection<U>> implements Collection<U>, Serializable {
	private static final long serialVersionUID = 8698249863871832402L;

	private transient org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.Initializor<T> initializor;
	protected T delegate;

	protected CollectionProxy() {
	}

	public CollectionProxy(Initializor<T> initializor) {
		this.initializor = initializor;
	}

	protected void checkInit() {
		if ( delegate == null ) {
			delegate = initializor.initialize();
		}
	}

	@Override
	public int size() {
		checkInit();
		return delegate.size();
	}

	@Override
	public boolean isEmpty() {
		checkInit();
		return delegate.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		checkInit();
		return delegate.contains( o );
	}

	@Override
	public Iterator<U> iterator() {
		checkInit();
		return delegate.iterator();
	}

	@Override
	public Object[] toArray() {
		checkInit();
		return delegate.toArray();
	}

	@Override
	public <V> V[] toArray(V[] a) {
		checkInit();
		return delegate.toArray( a );
	}

	@Override
	public boolean add(U o) {
		checkInit();
		return delegate.add( o );
	}

	@Override
	public boolean remove(Object o) {
		checkInit();
		return delegate.remove( o );
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		checkInit();
		return delegate.containsAll( c );
	}

	@Override
	public boolean addAll(Collection<? extends U> c) {
		checkInit();
		return delegate.addAll( c );
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		checkInit();
		return delegate.removeAll( c );
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		checkInit();
		return delegate.retainAll( c );
	}

	@Override
	public void clear() {
		checkInit();
		delegate.clear();
	}

	@Override
	public String toString() {
		checkInit();
		return delegate.toString();
	}

	@SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
	@Override
	public boolean equals(Object obj) {
		checkInit();
		return delegate.equals( obj );
	}

	@Override
	public int hashCode() {
		checkInit();
		return delegate.hashCode();
	}
}
