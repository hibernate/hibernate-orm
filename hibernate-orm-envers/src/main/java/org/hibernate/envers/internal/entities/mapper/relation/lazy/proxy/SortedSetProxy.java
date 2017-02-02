/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation.lazy.proxy;

import java.util.Comparator;
import java.util.SortedSet;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class SortedSetProxy<U> extends CollectionProxy<U, SortedSet<U>> implements SortedSet<U> {
	private static final long serialVersionUID = 2092884107178125905L;

	public SortedSetProxy() {
	}

	public SortedSetProxy(org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.Initializor<SortedSet<U>> initializor) {
		super( initializor );
	}

	@Override
	public Comparator<? super U> comparator() {
		checkInit();
		return delegate.comparator();
	}

	@Override
	public SortedSet<U> subSet(U u, U u1) {
		checkInit();
		return delegate.subSet( u, u1 );
	}

	@Override
	public SortedSet<U> headSet(U u) {
		checkInit();
		return delegate.headSet( u );
	}

	@Override
	public SortedSet<U> tailSet(U u) {
		checkInit();
		return delegate.tailSet( u );
	}

	@Override
	public U first() {
		checkInit();
		return delegate.first();
	}

	@Override
	public U last() {
		checkInit();
		return delegate.last();
	}
}
