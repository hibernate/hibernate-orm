/*
 * Envers. http://www.jboss.org/envers
 *
 * Copyright 2008  Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT A WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.entities.mapper.relation.lazy.proxy;

import java.util.List;
import java.util.Collection;
import java.util.ListIterator;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ListProxy<U> extends CollectionProxy<U, List<U>> implements List<U> {
    public ListProxy(org.jboss.envers.entities.mapper.relation.lazy.initializor.Initializor<List<U>> initializor) {
        super(initializor);
    }

    public boolean addAll(int index, Collection<? extends U> c) {
        checkInit();
        return delegate.addAll(index, c);
    }

    public U get(int index) {
        checkInit();
        return delegate.get(index);
    }

    public U set(int index, U element) {
        checkInit();
        return delegate.set(index, element);
    }

    public void add(int index, U element) {
        checkInit();
        delegate.add(index, element);
    }

    public U remove(int index) {
        checkInit();
        return delegate.remove(index);
    }

    public int indexOf(Object o) {
        checkInit();
        return delegate.indexOf(o);
    }

    public int lastIndexOf(Object o) {
        checkInit();
        return delegate.lastIndexOf(o);
    }

    public ListIterator<U> listIterator() {
        checkInit();
        return delegate.listIterator();
    }

    public ListIterator<U> listIterator(int index) {
        checkInit();
        return delegate.listIterator(index);
    }

    public List<U> subList(int fromIndex, int toIndex) {
        checkInit();
        return delegate.subList(fromIndex, toIndex);
    }
}
