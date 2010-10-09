/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.envers.entities.mapper.relation.lazy.proxy;

import java.util.Comparator;
import java.util.SortedSet;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class SortedSetProxy<U> extends CollectionProxy<U, SortedSet<U>> implements SortedSet<U> {
    private static final long serialVersionUID = 2092884107178125905L;

    public SortedSetProxy() {
    }

    public SortedSetProxy(org.hibernate.envers.entities.mapper.relation.lazy.initializor.Initializor<SortedSet<U>> initializor) {
        super(initializor);
    }

    public Comparator<? super U> comparator() {
        checkInit();
        return delegate.comparator();
    }

    public SortedSet<U> subSet(U u, U u1) {
        checkInit();
        return delegate.subSet(u, u1);
    }

    public SortedSet<U> headSet(U u) {
        checkInit();
        return delegate.headSet(u);
    }

    public SortedSet<U> tailSet(U u) {
        checkInit();
        return delegate.tailSet(u);
    }

    public U first() {
        checkInit();
        return delegate.first();
    }

    public U last() {
        checkInit();
        return delegate.last();
    }
}
