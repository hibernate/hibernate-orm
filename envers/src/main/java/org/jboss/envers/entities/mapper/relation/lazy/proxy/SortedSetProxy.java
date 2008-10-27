/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2008, Red Hat Middleware LLC, and others contributors as indicated
 * by the @authors tag. All rights reserved.
 *
 * See the copyright.txt in the distribution for a  full listing of individual
 * contributors. This copyrighted material is made available to anyone wishing
 * to use,  modify, copy, or redistribute it subject to the terms and
 * conditions of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT A WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.entities.mapper.relation.lazy.proxy;

import java.util.SortedSet;
import java.util.Comparator;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class SortedSetProxy<U> extends CollectionProxy<U, SortedSet<U>> implements SortedSet<U> {
    public SortedSetProxy(org.jboss.envers.entities.mapper.relation.lazy.initializor.Initializor<SortedSet<U>> initializor) {
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