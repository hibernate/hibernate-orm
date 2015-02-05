/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) ${year}, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.collection.custom.declaredtype.explicitsemantics;

import java.util.ArrayList;

/**
 * A custom collection class that has both List and Set interfaces, but only really implements
 * set for persistence (e.g. extends PersistentSet).
 * Without setting the semantics on the CollectionType annotation, List semantics would be be inferred,
 * and that would not match the implemented methods in PersistentSet and would fail.
 * HeadSetList is very much a toy collection type.
 * 
 * @author David Weinberg
 */
public class HeadSetList<X> extends ArrayList<X> implements IHeadSetList<X> {
    @Override
    public X head() {
        return isEmpty() ? null : get(0);
    }
}
